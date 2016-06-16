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
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * This command is used to terminate a single connection or multiple connections at the same time.<br>
 * As a side effect, it collects statistics on the execution of the connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(DeleteConnectionCommand.class);

    // MGCP Command Execution
    private int transactionId = 0;
    private int callId = 0;
    private int connectionId = 0;
    private String endpointId;
    private MgcpEndpoint endpoint;
    private int rxPackets = 0;
    private int txPackets = 0;

    public DeleteConnectionCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager, connectionProvider);
    }

    private void validateRequest(MgcpRequest request) throws MgcpCommandException, RuntimeException {
        this.transactionId = request.getTransactionId();

        // Call Identifier
        final String callIdHex = request.getParameter(MgcpParameterType.CALL_ID);
        if (callIdHex == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(),
                    MgcpResponseCode.INCORRECT_CALL_ID.message());
        } else {
            this.callId = Integer.parseInt(callIdHex, 16);
        }

        // Connection Identifier
        final String connectionIdHex = request.getParameter(MgcpParameterType.CONNECTION_ID);
        if (connectionIdHex != null) {
            this.connectionId = Integer.parseInt(connectionIdHex, 16);
        }

        // Endpoint Identifier
        final String endpointName = request.getEndpointId();
        if (endpointName.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        } else if (connectionIdHex != null && endpointName.indexOf(WILDCARD_ALL) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        } else {
            this.endpointId = endpointName.substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        }
    }

    private void executeCommand() throws MgcpCommandException, MgcpCallNotFoundException, MgcpConnectionNotFound {
        // Retrieve endpoint
        this.endpoint = this.endpointManager.getEndpoint(this.endpointId);
        if (this.endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(),
                    MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        if (this.connectionId > 0) {
            // Delete specific connection
            MgcpConnection deleted = this.endpoint.deleteConnection(this.callId, this.connectionId);
            // TODO Gather statistics from connection
        } else {
            // Bulk delete connections
            try {
                this.endpoint.deleteConnections(this.callId);
            } catch (MgcpCallNotFoundException e) {
                /*
                 * https://tools.ietf.org/html/rfc3435#section-2.3.9
                 * 
                 * Note that the command will still succeed if there were no connections with the CallId specified, as long as
                 * the EndpointId was valid.
                 */
            }
        }
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        response.setTransactionId(this.transactionId);

        if (this.connectionId > 0) {
            response.addParameter(MgcpParameterType.CONNECTION_PARAMETERS, "PS=" + txPackets + ", PR=" + rxPackets);
        }
        return response;
    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        try {
            validateRequest(request);
            executeCommand();
            return buildResponse();
        } catch (MgcpConnectionNotFound e) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(),
                    MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        } catch (MgcpCallNotFoundException e) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(),
                    MgcpResponseCode.INCORRECT_CALL_ID.message());
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Could not process MGCP Request.", e);
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
        this.endpointId = null;
        this.endpoint = null;
        this.rxPackets = 0;
        this.txPackets = 0;
    }

}
