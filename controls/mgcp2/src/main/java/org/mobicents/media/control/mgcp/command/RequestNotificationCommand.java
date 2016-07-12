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

package org.mobicents.media.control.mgcp.command;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * The NotificationRequest command is used to request the gateway to send notifications upon the occurrence of specified events
 * in an endpoint.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(RequestNotificationCommand.class);

    // MGCP Command Execution
    private int transactionId = 0;
    private int callId = 0;
    private String endpointId;
    private MgcpEndpoint endpoint;
    private String notifiedEntity;
    private String requestIdentifier;
    private String[] requestedEvents;
    private String signalRequests;

    public RequestNotificationCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager, connectionProvider);
    }

    private void validateEndpointId(String endpointId) throws MgcpCommandException {
        if (endpointId.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
    }

    private void validateRequest(MgcpRequest request) throws MgcpCommandException, RuntimeException {
        this.transactionId = request.getTransactionId();

        // Call ID
        String callId = request.getParameter(MgcpParameterType.CALL_ID);
        if (callId == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), "Call ID (C) is not specified");
        } else {
            this.callId = Integer.parseInt(callId, 16);
        }

        // Endpoint Name
        this.endpointId = request.getEndpointId().substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        validateEndpointId(this.endpointId);

        // Notified Entity
        this.notifiedEntity = request.getParameter(MgcpParameterType.NOTIFIED_ENTITY);

        // Requested Events
        String events = request.getParameter(MgcpParameterType.REQUESTED_EVENTS);
        if (events != null) {
            this.requestedEvents = events.split(",");
        }

        // Requested Signals
        this.signalRequests = request.getParameter(MgcpParameterType.REQUESTED_SIGNALS);
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        response.setTransactionId(this.transactionId);
        return response;
    }

    private void executeCommand() throws MgcpCommandException {
        // Retrieve endpoint
        this.endpoint = this.endpointManager.getEndpoint(this.endpointId);
        if (this.endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(),
                    MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }
        
        // TODO Check if connection is specified (to do later down in roadmap)
        
       // Register types of event to be looked at
       this.endpoint.listen(this.requestedEvents);
       
       
       this.endpoint.execute(signal);
       

    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        try {
            validateRequest(request);
            executeCommand();
            return buildResponse();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    @Override
    protected MgcpResponse rollback(int transactionId, int code, String message) {
        MgcpResponse response = new MgcpResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setTransactionId(transactionId);
        return response;
    }

    @Override
    protected void reset() {
        this.transactionId = 0;
        this.callId = 0;
        this.endpointId = null;
        this.endpoint = null;
        this.notifiedEntity = null;
        this.requestIdentifier = null;
        this.requestedEvents = null;
        this.signalRequests = null;

    }

}
