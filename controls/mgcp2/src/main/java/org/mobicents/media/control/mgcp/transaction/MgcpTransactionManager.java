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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManager implements MgcpTransactionListener {

    private static final Logger log = Logger.getLogger(MgcpTransactionManager.class);

    // MGCP Components
    private final MgcpTransactionProvider transactionProvider;
    private final MgcpMessageListener messageListener;

    // MGCP Transaction Manager
    private final ConcurrentHashMap<Integer, MgcpTransaction> transactions;
    private final AtomicInteger idGenerator;
    private final int minId;
    private final int maxId;

    public MgcpTransactionManager(MgcpMessageListener messageListener, MgcpTransactionProvider transactionProvider) {
        // MGCP Components
        this.messageListener = messageListener;
        this.transactionProvider = transactionProvider;

        // MGCP Transaction Manager
        this.minId = 1;
        this.maxId = 100000000;
        this.idGenerator = new AtomicInteger(this.minId);
        this.transactions = new ConcurrentHashMap<>(500);
    }

    private synchronized void verifyIdRange() {
        if (this.idGenerator.get() > maxId) {
            this.idGenerator.set(this.minId);
        }
    }

    private int generateId() {
        verifyIdRange();
        return this.idGenerator.getAndIncrement();
    }

    boolean isLocal(int transactionId) {
        return transactionId >= this.minId && transactionId <= this.maxId;
    }

    private MgcpTransaction createTransaction() throws DuplicateMgcpTransactionException {
        return createTransaction(generateId());
    }

    private MgcpTransaction createTransaction(int transactionId) throws DuplicateMgcpTransactionException {
        // Create Transaction
        MgcpTransaction transaction = this.transactionProvider.provide();
        transaction.setId(transactionId);
        transaction.addMessageListener(this.messageListener);
        transaction.addTransactionListener(this);

        // Register Transaction
        MgcpTransaction old = this.transactions.putIfAbsent(transactionId, transaction);

        // Ensure transaction is not duplicate
        if (old != null) {
            throw new DuplicateMgcpTransactionException("Transaction " + transactionId + " already exists.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Created new transaction " + transactionId + " to process request");
        }

        return transaction;
    }
    
    boolean contains(int transactionId) {
        return this.transactions.containsKey(transactionId);
    }

    private MgcpTransaction findTransaction(int transactionId) {
        return this.transactions.get(transactionId);
    }

    public void process(MgcpMessage message, MessageDirection direction) {
        int transactionId = message.getTransactionId();

        if (message.isRequest()) {
            // Create new transaction to process incoming request
            MgcpTransaction transaction;
            try {
                transaction = MessageDirection.INBOUND.equals(direction) ? createTransaction(transactionId)
                        : createTransaction();
                transaction.processRequest((MgcpRequest) message, direction);
            } catch (DuplicateMgcpTransactionException e) {
                // Send provisional response
                final MgcpResponseCode responseCode = MgcpResponseCode.TRANSACTION_BEEN_EXECUTED;
                sendResponse(transactionId, responseCode.code(), responseCode.message());
            }
        } else {
            MgcpTransaction transaction = findTransaction(transactionId);
            if (transaction == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Could not find transaction " + transactionId + " to process response.");
                }

                // Send erroneous response
                sendResponse(transactionId, MgcpResponseCode.PROTOCOL_ERROR.code(),
                        "Transaction " + transactionId + " does not exist");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Found transaction " + transactionId + " to process response.");
                }
                transaction.processResponse((MgcpResponse) message);
            }
        }
    }

    private void sendResponse(int transactionId, int code, String message) {
        MgcpResponse response = new MgcpResponse();
        response.setTransactionId(transactionId);
        response.setCode(code);
        response.setMessage(message);
        this.messageListener.onOutgoingMessage(response);
    }

    @Override
    public void onTransactionComplete(MgcpTransaction transaction) {
        if (log.isDebugEnabled()) {
            log.debug("Unregistered transaction " + transaction.getId());
        }

        // Unregister transaction
        this.transactions.remove(transaction.getId());

        // Unregister listeners from transaction
        transaction.removeMessageListener(this.messageListener);
        transaction.removeTransactionListener(this);
    }

}
