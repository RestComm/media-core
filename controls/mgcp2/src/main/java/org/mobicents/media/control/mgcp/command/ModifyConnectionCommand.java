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
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * This command is used to modify the characteristics of a gateway's "view" of a connection.<br>
 * This "view" of the call includes both the local connection descriptor as well as the remote connection descriptor.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(ModifyConnectionCommand.class);

    // MGCP Command Execution
    private int transactionId = 0;
    private int callId = 0;
    private int connectionId = 0;
    private String remoteSdp = null;
    private String localSdp = null;
    private ConnectionMode mode = null;
    private String endpointId;
    private MgcpEndpoint endpoint;
    private MgcpConnection connection;

    public ModifyConnectionCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager, connectionProvider);
    }

    private void validateRequest(MgcpRequest request) throws MgcpCommandException, RuntimeException {
        this.transactionId = request.getTransactionId();

        // Endpoint ID
        final String endpointName = request.getEndpointId();
        if (endpointName.indexOf(WILDCARD_ALL) != -1 || endpointName.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        } else {
            this.endpointId = endpointName.substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        }

        // Call ID
        final String callIdHex = request.getParameter(MgcpParameterType.CALL_ID);
        if (callIdHex == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), "Call ID (C) is not specified");
        } else {
            this.callId = Integer.parseInt(callIdHex, 16);
        }

        // TODO Local Connection Options

        // Connection ID
        final String connectionIdHex = request.getParameter(MgcpParameterType.CONNECTION_ID);
        if (connectionIdHex == null || connectionIdHex.isEmpty()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(),
                    MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        } else {
            this.connectionId = Integer.parseInt(connectionIdHex, 16);
        }

        // Connection Mode
        final String connectionMode = request.getParameter(MgcpParameterType.MODE);
        if (connectionMode != null) {
            try {
                this.mode = ConnectionMode.fromDescription(connectionMode);
            } catch (IllegalArgumentException e) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(),
                        MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.message());
            }
        }

        // Remote Description
        this.remoteSdp = request.getParameter(MgcpParameterType.SDP);
    }

    private void executeCommand() throws MgcpCommandException {
        // Retrieve endpoint
        this.endpoint = this.endpointManager.getEndpoint(this.endpointId);
        if (this.endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(),
                    MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        // Retrieve connection from endpoint
        this.connection = this.endpoint.getConnection(this.callId, this.connectionId);
        if (this.connection == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(),
                    MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        }

        // Set Mode (if specified)
        if (this.mode != null) {
            this.connection.setMode(this.mode);
        }

        // Set Remote Description (if defined)
        if (this.remoteSdp != null) {
            try {
                this.localSdp = this.connection.open(this.remoteSdp);
            } catch (MgcpConnectionException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNSUPPORTED_SDP.code(),
                        MgcpResponseCode.UNSUPPORTED_SDP.message());
            }
        }
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        response.setTransactionId(this.transactionId);
        if (this.localSdp != null) {
            response.addParameter(MgcpParameterType.SDP, this.localSdp);
        }
        return response;
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
        this.connectionId = 0;
        this.remoteSdp = null;
        this.localSdp = null;
        this.mode = null;
        this.endpoint = null;
        this.connection = null;
    }

}
