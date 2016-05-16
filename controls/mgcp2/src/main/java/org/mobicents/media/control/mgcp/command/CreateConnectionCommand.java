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
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.local.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommand extends AbstractMgcpCommand {
    
    private static final Logger log = Logger.getLogger(CreateConnectionCommand.class);

    private static final String WILDCARD_ANY = "*";
    private static final String WILDCARD_ALL = "$";
    private static final String ENDPOINT_ID_SEPARATOR = "@";

    // MGCP Components
    private final MgcpEndpointManager endpointManager;

    // MGCP Command Execution
    private int transactionId = 0;
    private String remoteSdp = null;
    private String localSdp = null;
    private int callId = 0;
    private ConnectionMode mode = null;
    private MgcpEndpoint endpoint1;
    private MgcpEndpoint endpoint2;
    private MgcpConnection connection1;
    private MgcpConnection connection2;

    public CreateConnectionCommand(MgcpEndpointManager endpointManager) {
        // MGCP Components
        this.endpointManager = endpointManager;
    }

    private void validateRequest(MgcpRequest request) throws MgcpCommandException, RuntimeException {
        this.transactionId = request.getTransactionId();

        String z2 = request.getParameter(MgcpParameterType.SECOND_ENDPOINT);
        this.remoteSdp = request.getParameter(MgcpParameterType.SDP);

        // Z2 and SDP must not be together in same request
        if (z2 != null && this.remoteSdp != null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Z2 and SDP present in message");
        }

        // Call ID
        String callId = request.getParameter(MgcpParameterType.CALL_ID);
        if (callId == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), "Call ID (C) is not specified");
        } else {
            this.callId = Integer.parseInt(callId);
        }

        // Connection Mode
        try {
            this.mode = ConnectionMode.fromDescription(request.getParameter(MgcpParameterType.MODE));
        } catch (IllegalArgumentException e) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(),
                    "Connection Mode (M) not specified");
        }

        // Endpoint Name
        String endpointId = request.getEndpointId().substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        validateEndpointId(endpointId);

        // Secondary Endpoint Name
        String secondaryEndpointId = null;
        if (z2 != null) {
            secondaryEndpointId = z2.substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
            validateEndpointId(secondaryEndpointId);
        }

        // Retrieve Endpoints
        this.endpoint1 = resolveEndpoint(endpointId);
        this.endpoint2 = (secondaryEndpointId == null) ? null : resolveEndpoint(secondaryEndpointId);
    }

    private void executeCommand() throws MgcpConnectionException {

        if (this.endpoint2 == null) {
            // Create one connection between endpoint and remote peer
            this.connection1 = this.endpoint1.createConnection(this.callId, this.mode);
            // TODO set call agent
            // TODO get local connection options and pass them to halfOpen method
            this.localSdp = (this.remoteSdp == null) ? connection1.halfOpen(null) : connection1.open(this.remoteSdp);
        } else {
            // Create two local connections between both endpoints
            this.connection1 = endpoint1.createConnection(this.callId, this.mode, this.endpoint2);
            this.connection2 = endpoint2.createConnection(this.callId, ConnectionMode.SEND_RECV, this.endpoint1);
            ((MgcpLocalConnection) connection1).join((MgcpLocalConnection) connection2);
        }
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage("Transaction was executed successfully");
        response.setTransactionId(this.transactionId);
        response.addParameter(MgcpParameterType.ENDPOINT_ID, this.endpoint1.getEndpointId());
        response.addParameter(MgcpParameterType.CONNECTION_ID, this.connection1.getHexIdentifier());
        if (this.endpoint2 != null) {
            response.addParameter(MgcpParameterType.SECOND_ENDPOINT, this.endpoint2.getEndpointId());
            response.addParameter(MgcpParameterType.CONNECTION_ID2, this.connection2.getHexIdentifier());
        }
        if (this.localSdp != null) {
            response.addParameter(MgcpParameterType.SDP, this.localSdp);
        }
        return response;
    }

    private void validateEndpointId(String endpointId) throws MgcpCommandException {
        if (endpointId.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    "Wildcard ALL (*) is not supported");
        }
    }

    private MgcpEndpoint resolveEndpoint(String endpointId) throws MgcpCommandException {
        MgcpEndpoint endpoint;
        int indexOfAll = endpointId.indexOf(WILDCARD_ALL);
        if (indexOfAll == -1) {
            // Search for registered endpoint
            int separator = endpointId.indexOf(ENDPOINT_ID_SEPARATOR);
            endpoint = this.endpointManager.getEndpoint(endpointId.substring(0, separator));

            if (endpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(),
                        "Endpoint " + endpointId + " not found");
            }
        } else {
            // Create new endpoint for a specific name space
            int separator = endpointId.indexOf(ENDPOINT_ID_SEPARATOR);
            try {
                endpoint = this.endpointManager.registerEndpoint(endpointId.substring(0, separator));
            } catch (UnrecognizedMgcpNamespaceException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), e.getMessage());
            }
        }
        return endpoint;
    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        try {
            validateRequest(request);
            executeCommand();
            return buildResponse();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException | MgcpConnectionException e) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Could not process request");
        } finally {
            reset();
        }
    }

    @Override
    protected MgcpResponse rollback(int transactionId, int code, String message) {
        if (endpoint1 != null && connection1 != null) {
            try {
                this.endpoint1.deleteConnection(this.callId, this.connection1.getIdentifier());
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.warn("Could not delete primary connection. " + e.getMessage());
            }
        }

        if (endpoint2 != null && connection2 != null) {
            try {
                this.endpoint2.deleteConnection(this.callId, this.connection2.getIdentifier());
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.warn("Could not delete secondary connection. " + e.getMessage());
            }
        }

        MgcpResponse response = new MgcpResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setTransactionId(transactionId);
        return response;
    }

    private void reset() {
        this.transactionId = 0;
        this.remoteSdp = null;
        this.localSdp = null;
        this.callId = 0;
        this.mode = null;
        this.endpoint1 = null;
        this.endpoint2 = null;
        this.connection1 = null;
        this.connection2 = null;
    }

}
