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
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * This command is used to create a connection between two endpoints.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(CreateConnectionCommand.class);

    // MGCP Command Execution
    private int transactionId = 0;
    private String remoteSdp = null;
    private String localSdp = null;
    private int callId = 0;
    private ConnectionMode mode = null;
    private String endpointId;
    private String secondaryEndpointId;
    private MgcpEndpoint endpoint1;
    private MgcpEndpoint endpoint2;
    private MgcpConnection connection1;
    private MgcpConnection connection2;
    
    private final MgcpConnectionProvider connectionProvider;

    public CreateConnectionCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager);
        this.connectionProvider = connectionProvider;
    }

    /**
     * Creates a new Remote Connection.
     * 
     * <p>
     * The connection will be half-open and a Local Connection Description is generated.
     * </p>
     * 
     * @param callId The call identifies which indicates to which session the connection belongs to.
     * @param mode The connection mode.
     * @param endpoint The endpoint where the connection will be registered to.
     * 
     * @return The new connection
     * @throws MgcpConnectionException If connection could not be half opened.
     */
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, MgcpEndpoint endpoint)
            throws MgcpConnectionException {
        // Create connection
        MgcpRemoteConnection connection = this.connectionProvider.provideRemote();
        // TODO set call agent
        connection.setMode(mode);
        // TODO provide local connection options
        this.localSdp = connection.halfOpen(new LocalConnectionOptions());

        // Register connection under its proper call
        endpoint.addConnection(callId, connection);

        return connection;
    }

    /**
     * Creates a new Remote Connection.
     * 
     * <p>
     * The connection will be fully open and connected to the remote peer.<br>
     * A Local Connection Description is generated.
     * </p>
     * 
     * @param callId The the call identifies which indicates to which session the connection belongs to.
     * @param mode The connection mode.
     * @param remoteDescription The description of the remote connection.
     * @param endpoint The endpoint where the connection will be registered to.
     * 
     * @return The new connection
     * @throws MgcpConnectionException If connection could not be opened
     */
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, String remoteDescription,
            MgcpEndpoint endpoint) throws MgcpConnectionException {
        // Create connection
        MgcpRemoteConnection connection = this.connectionProvider.provideRemote();
        // TODO set call agent
        this.localSdp = connection.open(remoteDescription);
        connection.setMode(mode);

        // Register connection under its proper call
        endpoint.addConnection(callId, connection);

        return connection;
    }

    /**
     * Creates a new Local Connection.
     * 
     * <p>
     * The connection will be fully open and connected to a secondary endpoint.<br>
     * </p>
     * 
     * @param callId The the call identifies which indicates to which session the connection belongs to.
     * @param mode The connection mode.
     * @param secondEndpoint The endpoint where the connection will be registered to.
     * 
     * @return The new connection
     * @throws MgcpException If connection could not be opened.
     */
    private MgcpConnection createLocalConnection(int callId, ConnectionMode mode, MgcpEndpoint endpoint)
            throws MgcpConnectionException {
        // Create connection
        MgcpLocalConnection connection = this.connectionProvider.provideLocal();
        connection.open(null);
        connection.setMode(mode);

        // Register connection under its proper call
        endpoint.addConnection(callId, connection);

        return connection;
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
            this.callId = Integer.parseInt(callId, 16);
        }

        // Connection Mode
        try {
            this.mode = ConnectionMode.fromDescription(request.getParameter(MgcpParameterType.MODE));
        } catch (IllegalArgumentException e) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(),
                    "Connection Mode (M) not specified");
        }

        // Endpoint Name
        this.endpointId = request.getEndpointId().substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        validateEndpointId(this.endpointId);

        // Secondary Endpoint Name
        this.secondaryEndpointId = null;
        if (z2 != null) {
            this.secondaryEndpointId = z2.substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
            validateEndpointId(this.secondaryEndpointId);
        }
    }

    private void executeCommand() throws MgcpConnectionException, MgcpCommandException {
        // Retrieve Endpoints
        this.endpoint1 = retrieveEndpoint(this.endpointId);
        this.endpoint2 = retrieveEndpoint(this.secondaryEndpointId);

        // Create Connections
        if (this.endpoint2 == null) {
            if (this.remoteSdp == null) {
                // Create half-open connection
                this.connection1 = createRemoteConnection(this.callId, this.mode, this.endpoint1);
            } else {
                // Create open connection
                this.connection1 = createRemoteConnection(this.callId, this.mode, this.remoteSdp, this.endpoint1);
            }
        } else {
            // Create two local connections between both endpoints
            this.connection1 = createLocalConnection(this.callId, mode, endpoint1);
            this.connection2 = createLocalConnection(callId, ConnectionMode.SEND_RECV, endpoint2);

            // Join connections
            ((MgcpLocalConnection) connection1).join((MgcpLocalConnection) connection2);
        }
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        response.setTransactionId(this.transactionId);
        // XXX do not hardcode the endpoint address
        response.addParameter(MgcpParameterType.ENDPOINT_ID, this.endpoint1.getEndpointId() + "@127.0.0.1:2427");
        response.addParameter(MgcpParameterType.CONNECTION_ID, this.connection1.getHexIdentifier());
        if (this.endpoint2 != null) {
            // XXX do not hardcode the endpoint address
            response.addParameter(MgcpParameterType.SECOND_ENDPOINT, this.endpoint2.getEndpointId() + "@127.0.0.1:2427");
            response.addParameter(MgcpParameterType.CONNECTION_ID2, this.connection2.getHexIdentifier());
        }
        if (this.localSdp != null) {
            response.addParameter(MgcpParameterType.SDP, this.localSdp);
        }
        return response;
    }

    private void validateEndpointId(String endpointId) throws MgcpCommandException {
        if (endpointId.indexOf(WILDCARD_ALL) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
    }

    private MgcpEndpoint retrieveEndpoint(String endpointId) throws MgcpCommandException {
        if (endpointId == null || endpointId.isEmpty()) {
            return null;
        }

        MgcpEndpoint endpoint;
        int indexOfAll = endpointId.indexOf(WILDCARD_ANY);
        if (indexOfAll == -1) {
            // Search for registered endpoint
            endpoint = this.endpointManager.getEndpoint(endpointId);

            if (endpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(),
                        MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.message());
            }
        } else {
            // Create new endpoint for a specific name space
            try {
                endpoint = this.endpointManager.registerEndpoint(endpointId.substring(0, indexOfAll));
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
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Could not process request");
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

    protected void reset() {
        this.transactionId = 0;
        this.remoteSdp = null;
        this.localSdp = null;
        this.callId = 0;
        this.mode = null;
        this.endpointId = null;
        this.secondaryEndpointId = null;
        this.endpoint1 = null;
        this.endpoint2 = null;
        this.connection1 = null;
        this.connection2 = null;
    }

}
