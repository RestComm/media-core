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

import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpCommandListener;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.network.MgcpChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransaction implements MgcpCommandListener {

    // Mgcp Components
    private final MgcpCommandProvider commands;
    private final MgcpChannel channel;
    private final MgcpTransactionListener listener;

    // MGCP Transaction State
    private int id;
    private String hexId;
    private MessageDirection direction;
    private MgcpTransactionState state;

    public MgcpTransaction(MgcpCommandProvider commands, MgcpChannel channel, MgcpTransactionListener listener) {
        // MGCP Components
        this.commands = commands;
        this.channel = channel;
        this.listener = listener;

        // MGCP Transaction State
        this.id = 0;
        this.direction = null;
        this.state = MgcpTransactionState.IDLE;
    }

    public int getId() {
        return id;
    }

    public String getHexId() {
        return hexId;
    }

    public void setId(int id) {
        this.id = id;
        this.hexId = Integer.toHexString(id);
    }
    
    public MgcpTransactionState getState() {
        return state;
    }

    private void sendMessage(MgcpMessage message) {
        byte[] data = message.toString().getBytes();
        this.channel.queue(data);
    }

    public void processRequest(MgcpRequest request, MessageDirection direction) throws IllegalStateException {
        switch (this.state) {
            case IDLE:
                this.direction = direction;
                this.state = MgcpTransactionState.EXECUTING_REQUEST;

                switch (direction) {
                    case INBOUND:
                        // Execute incoming MGCP request
                        MgcpCommand command = this.commands.provide(request);
                        command.execute(request);
                        // Transaction must now listen for onCommandComplete event
                        break;

                    case OUTBOUND:
                        // Send the request to the remote peer right now and wait for the response
                        sendMessage(request);
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
                    sendMessage(response);
                }
                this.listener.onTransactionComplete(this);
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
