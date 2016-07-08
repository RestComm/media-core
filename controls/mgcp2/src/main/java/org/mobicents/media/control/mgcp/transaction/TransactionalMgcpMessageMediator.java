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

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageMediator;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * Manages a group of MGCP transactions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TransactionalMgcpMessageMediator extends MgcpMessageMediator {

    private static final Logger log = Logger.getLogger(TransactionalMgcpMessageMediator.class);

    // MGCP Components
    private final MgcpTransactionProvider transactionProvider;
    private final MgcpCommandProvider commands;

    // MGCP Transaction Manager
    private final ConcurrentHashMap<Integer, MgcpTransaction> transactions;

    public TransactionalMgcpMessageMediator(MgcpTransactionProvider transactionProvider, MgcpCommandProvider commands) {
        // MGCP Components
        this.transactionProvider = transactionProvider;
        this.commands = commands;

        // MGCP Transaction Manager
        this.transactions = new ConcurrentHashMap<>(500);
    }

    private MgcpTransaction createTransaction(int transactionId) throws DuplicateMgcpTransactionException {
        // Create Transaction
        MgcpTransaction transaction;
        if (transactionId == 0) {
            // Transaction originated from within this Media Server (NTFY, for example)
            // to be sent out to call agent. A transaction ID must be generated
            transaction = this.transactionProvider.provideLocal();
        } else {
            // Transaction originated from the remote call agent
            transaction = this.transactionProvider.provideRemote(transactionId);
        }

        // Register Transaction
        MgcpTransaction old = this.transactions.putIfAbsent(transaction.getId(), transaction);

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
    
    private void processRequest(Object originator, MgcpRequest request, MessageDirection direction) {
        switch (direction) {
            case INCOMING:
                // Execute incoming MGCP request
                MgcpCommand command = this.commands.provide(request.getRequestType());
                command.execute(request, this);
                // Transaction must now listen for onCommandComplete event
                break;

            case OUTGOING:
                // Send the request to the remote peer right now and wait for the response
                broadcast(originator, request, direction);
                break;

            default:
                throw new IllegalArgumentException("Unknown message direction: " + direction);
        }
    }
    
    public void processResponse(Object originator, MgcpResponse response, MessageDirection direction) {
        // Transaction completes once response is received
        // Locate transaction and close it.
        final int transactionId = response.getTransactionId();
        this.transactions.remove(transactionId);

        if (log.isInfoEnabled()) {
            log.info("Transaction " + transactionId + " terminated with code " + response.getCode());
        }

        if (MessageDirection.OUTGOING.equals(direction)) {
            // The response was originated from within the Media Server.
            // Broadcast it so the transport channel can be notified and send the message to call agent.
            broadcast(originator, response, direction);
        }
    }

    private void sendResponse(int transactionId, int code, String message) {
        MgcpResponse response = new MgcpResponse();
        response.setTransactionId(transactionId);
        response.setCode(code);
        response.setMessage(message);
        broadcast(this, response, MessageDirection.OUTGOING);
    }

    @Override
    public void notify(Object originator, MgcpMessage message, MessageDirection direction) {
        int transactionId = message.getTransactionId();
        if (message.isRequest()) {
            try {
                // Create new transaction to process incoming request
                createTransaction(transactionId);
                processRequest(originator, (MgcpRequest) message, direction);
            } catch (DuplicateMgcpTransactionException e) {
                // Send provisional response
                final MgcpResponseCode responseCode = MgcpResponseCode.TRANSACTION_BEEN_EXECUTED;
                sendResponse(transactionId, responseCode.code(), responseCode.message());
            }
        } else {
            // Locate transaction to which the response belongs to
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
                processResponse(originator, (MgcpResponse) message, direction);
            }
        }
    }

}
