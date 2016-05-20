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

import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpCommandListener;
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransaction implements MgcpCommandListener {

    // Mgcp Components
    private final MgcpCommandProvider commands;
    private final List<MgcpMessageListener> messageListeners;
    private final List<MgcpTransactionListener> transactionListeners;

    // MGCP Transaction State
    private final int id;
    private final String hexId;
    private MessageDirection direction;
    private MgcpTransactionState state;

    public MgcpTransaction(int id, MgcpCommandProvider commands) {
        // MGCP Components
        this.commands = commands;
        this.messageListeners = new ArrayList<>(5);
        this.transactionListeners = new ArrayList<>(5);

        // MGCP Transaction State
        this.id = id;
        this.hexId = Integer.toHexString(id);
        this.direction = null;
        this.state = MgcpTransactionState.IDLE;
    }

    public int getId() {
        return id;
    }

    public String getHexId() {
        return hexId;
    }

    public MgcpTransactionState getState() {
        return state;
    }
    
    public void addMessageListener(MgcpMessageListener listener) {
        this.messageListeners.add(listener);
    }

    public void removeMessageListener(MgcpMessageListener listener) {
        this.messageListeners.remove(listener);
    }
    
    public void addTransactionListener(MgcpTransactionListener listener) {
        this.transactionListeners.add(listener);
    }

    public void removeTransactionListener(MgcpTransactionListener listener) {
        this.transactionListeners.remove(listener);
    }

    private void broadcast(MgcpMessage message) {
        for (MgcpMessageListener observer : this.messageListeners) {
            observer.onOutgoingMessage(message);
        }
    }

    private void broadcast(MgcpTransaction transaction) {
        for (MgcpTransactionListener observer : this.transactionListeners) {
            observer.onTransactionComplete(transaction);
        }
    }

    public void processRequest(MgcpRequest request, MessageDirection direction) throws IllegalStateException {
        switch (this.state) {
            case IDLE:
                this.direction = direction;
                this.state = MgcpTransactionState.EXECUTING_REQUEST;

                switch (direction) {
                    case INBOUND:
                        // Execute incoming MGCP request
                        MgcpCommand command = this.commands.provide(request.getRequestType());
                        command.execute(request);
                        // Transaction must now listen for onCommandComplete event
                        break;

                    case OUTBOUND:
                        // Send the request to the remote peer right now and wait for the response
                        broadcast(request);
                        this.state = MgcpTransactionState.WAITING_RESPONSE;
                        break;

                    default:
                        throw new IllegalArgumentException("Unknown message direction: " + direction);
                }
                break;

            default:
                throw new IllegalStateException("Request cannot be processed because transaction is already " + this.state);
        }
    }

    public void processResponse(MgcpResponse response) throws IllegalStateException {
        switch (this.state) {
            case WAITING_RESPONSE:
                this.state = MgcpTransactionState.COMPLETED;
                if (MessageDirection.INBOUND.equals(this.direction)) {
                    // Command finished executing inbound request
                    // Time to send response to the remote peer
                    broadcast(response);
                }
                broadcast(this);
                break;

            default:
                throw new IllegalStateException("Request cannot be processed because transaction is " + this.state);
        }
    }

    @Override
    public void onCommandExecuted(MgcpResponse response) {
        this.state = MgcpTransactionState.WAITING_RESPONSE;
        processResponse(response);
    }

}
