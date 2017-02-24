/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.control.mgcp.transaction;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandResult;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SubMgcpTransactionManager implements MgcpTransactionManager {

    private static final Logger log = Logger.getLogger(SubMgcpTransactionManager.class);

    // Concurrency Components
    private final ListeningExecutorService executor;

    // MGCP Components
    private final MgcpTransactionNumberspace numberspace;

    // MGCP Transaction Manager (per call agent)
    private final ConcurrentHashMap<Integer, MgcpTransaction> transactions;
    /*
     * TODO MGCP entities MUST keep in memory a list of the responses that they sent to recent transactions, i.e., a list of all
     * the responses they sent over the last T-HIST seconds.
     * 
     * The transaction identifiers of incoming commands are compared to the transaction identifiers of the recent responses. If
     * a match is found, the MGCP entity does not execute the transaction, but simply repeats the response.
     */

    // Observers
    private final Set<MgcpMessageObserver> observers;

    public SubMgcpTransactionManager(MgcpTransactionNumberspace numberspace, ListeningExecutorService executor) {
        // Concurrency Components
        this.executor = executor;

        // MGCP Components
        this.numberspace = numberspace;

        // MGCP Transaction Manager
        this.transactions = new ConcurrentHashMap<>(500);

        // Observers
        this.observers = Sets.newConcurrentHashSet();
    }

    private MgcpTransaction createTransaction(MgcpRequest request) throws DuplicateMgcpTransactionException {
        int transactionId = request.getTransactionId();
        final boolean local = (transactionId == 0);

        // Create Transaction
        MgcpTransaction transaction;
        if (local) {
            // Transaction originated from within this Media Server (NTFY, for example)
            // to be sent out to call agent. A transaction ID must be generated
            transaction = new MgcpTransaction(this.numberspace.generateId());

            // Patch transaction ID
            request.setTransactionId(transaction.getId());
            transactionId = transaction.getId();
        } else {
            // Transaction originated from the remote call agent
            transaction = new MgcpTransaction(transactionId);
        }

        // Register Transaction
        MgcpTransaction old = this.transactions.putIfAbsent(transaction.getId(), transaction);

        // Ensure transaction is not duplicate
        if (old != null) {
            throw new DuplicateMgcpTransactionException("Transaction " + transactionId + " already exists.");
        }

        return transaction;
    }

    boolean contains(int transactionId) {
        return this.transactions.containsKey(transactionId);
    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpRequest request, MgcpCommand command, MessageDirection direction) throws DuplicateMgcpTransactionException {
        // TODO check message direction
        createTransaction(request);
        
        if (log.isDebugEnabled()) {
            String callAgent = MessageDirection.INCOMING.equals(direction) ? from.toString() : to.toString();
            log.debug("Started transaction " + request.getTransactionId() + " for call agent " + callAgent);
        }
        
        if (command != null) {
            ListenableFuture<MgcpCommandResult> future = this.executor.submit(command);
            Futures.addCallback(future, new MgcpCommandCallback(from, to, request.getTransactionId()));
        }
    }

    @Override
    public void process(InetSocketAddress from, InetSocketAddress to, MgcpResponse response, MessageDirection direction) throws MgcpTransactionNotFoundException {
     // TODO check message direction
        MgcpTransaction transaction = this.transactions.remove(response.getTransactionId());
        if (transaction == null) {
            throw new MgcpTransactionNotFoundException("Could not find transaction " + response.getTransactionId());
        } else {
            if (log.isDebugEnabled()) {
                String callAgent = MessageDirection.INCOMING.equals(direction) ? from.toString() : to.toString();
                log.debug("Closed transaction " + response.getTransactionId() + " for call agent " + callAgent + " with code " + response.getCode());
            }
        }
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.observers.add(observer);
        if (log.isTraceEnabled()) {
            log.trace("Registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.observers.remove(observer);
        if (log.isTraceEnabled()) {
            log.trace("Unregistered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

    /**
     * Handles MGCP command responses after their execution.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class MgcpCommandCallback implements FutureCallback<MgcpCommandResult> {
        
        private final InetSocketAddress from;
        private final InetSocketAddress to;
        private final int transactionId;
        
        public MgcpCommandCallback(InetSocketAddress from, InetSocketAddress to, int transactionId) {
            this.from = from;
            this.to = to;
            this.transactionId = transactionId;
        }

        @Override
        public void onSuccess(MgcpCommandResult result) {
            if(log.isTraceEnabled()) {
                log.trace("MGCP Command of transaction " + result.getTransactionId() + " executed successfully.");
            }
            MgcpResponse response = buildResponse(result);
            SubMgcpTransactionManager.this.notify(SubMgcpTransactionManager.this, this.to, this.from, response, MessageDirection.OUTGOING);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("MGCP Command of transaction " + this.transactionId + " failed. Replying with error code " + MgcpResponseCode.PROTOCOL_ERROR.code(), t);
            MgcpResponse response = buildResponse(MgcpResponseCode.PROTOCOL_ERROR);
            SubMgcpTransactionManager.this.notify(SubMgcpTransactionManager.this, this.to, this.from, response, MessageDirection.OUTGOING);
        }
        
        private MgcpResponse buildResponse(MgcpCommandResult result) {
            MgcpResponse response = new MgcpResponse();
            response.setCode(result.getCode());
            response.setMessage(result.getMessage());
            response.setTransactionId(result.getTransactionId());
            
            Parameters<MgcpParameterType> parameters = result.getParameters();
            if(parameters.size() > 0) {
                Iterator<MgcpParameterType> iterator = parameters.keySet().iterator();
                while (iterator.hasNext()) {
                    MgcpParameterType key = iterator.next();
                    Optional<String> value = parameters.getString(key);
                    if(value.isPresent()) {
                        response.addParameter(key, value.get());
                    }
                }
            }
            
            return response;
        }
        
        private MgcpResponse buildResponse(MgcpResponseCode code) {
            MgcpResponse response = new MgcpResponse();
            response.setCode(code.code());
            response.setMessage(code.message());
            response.setTransactionId(this.transactionId);
            return response;
        }

    }

}
