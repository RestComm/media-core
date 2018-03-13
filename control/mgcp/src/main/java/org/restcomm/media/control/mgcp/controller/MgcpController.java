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

package org.restcomm.media.control.mgcp.controller;

import java.net.InetSocketAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandProvider;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerEvent;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerFsm;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerFsmBuilder;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerFsmImpl;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerGlobalContext;
import org.restcomm.media.control.mgcp.controller.fsm.MgcpControllerState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.restcomm.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.network.netty.AsyncMgcpChannel;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionManager;
import org.restcomm.media.core.spi.ControlProtocol;
import org.restcomm.media.core.spi.Endpoint;
import org.restcomm.media.core.spi.EndpointInstaller;
import org.restcomm.media.core.spi.ServerManager;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpController implements ServerManager, MgcpMessageObserver {

    private static final Logger log = LogManager.getLogger(MgcpController.class);

    // MGCP Components
    private final MgcpTransactionManager transactions;
    private final MgcpEndpointManager endpoints;
    private final MgcpCommandProvider commands;

    // MGCP Controller State
    private final MgcpControllerGlobalContext context;
    private final MgcpControllerFsm fsm;

    public MgcpController(String address, int port, AsyncMgcpChannel channel, MgcpTransactionManager transactions, MgcpEndpointManager endpoints, MgcpCommandProvider commands) {
        // MGCP Components
        this.transactions = transactions;
        this.endpoints = endpoints;
        this.commands = commands;

        // MGCP Controller State
        this.context = new MgcpControllerGlobalContext();
        this.context.setBindAddress(new InetSocketAddress(address, port));
        this.context.setChannel(channel);
        this.fsm = MgcpControllerFsmBuilder.INSTANCE.build(this.context);
        this.fsm.start();
    }

    @Override
    public ControlProtocol getControlProtocol() {
        return ControlProtocol.MGPC;
    }

    @Override
    public void activate() throws IllegalStateException {
        if (isActive()) {
            throw new IllegalStateException("Controller is already active");
        } else {
            this.context.getChannel().observe(this);
            this.transactions.observe(this);
            this.endpoints.observe(this);

            this.fsm.fire(MgcpControllerEvent.ACTIVATE);
        }
    }

    @Override
    public void deactivate() throws IllegalStateException {
        if (isActive()) {
            // TODO elegant cleanup of existing resources
            this.context.getChannel().forget(this);
            this.transactions.forget(this);
            this.endpoints.observe(this);

            this.fsm.fire(MgcpControllerEvent.DEACTIVATE);
        } else {
            throw new IllegalStateException("Controller is already inactive");
        }
    }

    @Override
    public boolean isActive() {
        return MgcpControllerState.ACTIVATED.equals(this.fsm.getCurrentState());
    }

    @Override
    public void onStarted(Endpoint endpoint, EndpointInstaller installer) {
        // Legacy stuff
    }

    @Override
    public void onStopped(Endpoint endpoint) {
        // Legacy stuff
    }

    @Override
    public void onMessage(InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        switch (direction) {
            case INCOMING:
                if (message.isRequest()) {
                    onIncomingRequest(from, to, (MgcpRequest) message);
                } else {
                    onIncomingResponse(from, to, (MgcpResponse) message);
                }
                break;

            case OUTGOING:
                if (message.isRequest()) {
                    onOutgoingRequest(from, to, (MgcpRequest) message);
                } else {
                    onOutgoingResponse(from, to, (MgcpResponse) message);
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown message direction: " + direction.name());
        }
    }

    private void onIncomingRequest(InetSocketAddress from, InetSocketAddress to, MgcpRequest request) {
        // Get command to be executed
        MgcpCommand command = this.commands.provide(request.getRequestType(), request.getTransactionId(),
                request.getParameters());

        try {
            // Start transaction that will execute the command
            this.transactions.process(from, to, request, command, MessageDirection.INCOMING);
        } catch (DuplicateMgcpTransactionException e) {
            // Transaction is already being processed
            // Send provisional message
            MgcpResponseCode provisional = MgcpResponseCode.TRANSACTION_BEING_EXECUTED;

            if (log.isDebugEnabled()) {
                log.debug("Received duplicate request tx=" + request.getTransactionId() + " from " + from.toString()
                        + ". Sending provisional response with code " + provisional.code());
            }

            sendResponse(from, request.getTransactionId(), provisional.code(), provisional.message());
        }
    }

    private void onOutgoingRequest(InetSocketAddress from, InetSocketAddress to, MgcpRequest request) {
        try {
            // Start transaction
            this.transactions.process(from, to, request, null, MessageDirection.OUTGOING);
            // Send request to call agent
            this.context.getChannel().send(request, to, new SendChannelCallback());
        } catch (DuplicateMgcpTransactionException e) {
            log.error(e.getMessage() + ". Request wont' be sent to call agent.");
        }
    }

    private void onIncomingResponse(InetSocketAddress from, InetSocketAddress to, MgcpResponse response) {
        try {
            // Close transaction
            this.transactions.process(from, to, response, MessageDirection.INCOMING);
        } catch (MgcpTransactionNotFoundException e) {
            log.error(e.getMessage());
        }
    }

    private void onOutgoingResponse(InetSocketAddress from, InetSocketAddress to, MgcpResponse response) {
        try {
            // Close transaction
            this.transactions.process(from, to, response, MessageDirection.OUTGOING);
            // Send response to call agent
            this.context.getChannel().send(response, to, new SendChannelCallback());
        } catch (MgcpTransactionNotFoundException e) {
            log.error(e.getMessage() + ". Response won't be sent to call agent.");
        }
    }

    private void sendResponse(InetSocketAddress to, int transactionId, int code, String message) {
        MgcpResponse response = new MgcpResponse();
        response.setTransactionId(transactionId);
        response.setCode(code);
        response.setMessage(message);
        this.context.getChannel().send(response, to, new SendChannelCallback());
    }
    
    private class SendChannelCallback implements FutureCallback<Void> {

        @Override
        public void onSuccess(Void result) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Could not send MGCP message to remote peer.", t);
            
        }}

}
