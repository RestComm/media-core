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
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;
import org.mobicents.media.server.spi.ConnectionMode;

import com.google.common.base.Optional;

/**
 * This command is used to create a connection between two endpoints.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(CreateConnectionCommand.class);

    public CreateConnectionCommand(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager) {
        super(transactionId, parameters, endpointManager);
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
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, MgcpEndpoint endpoint) throws MgcpConnectionException {
        // Create connection
        MgcpConnection connection = endpoint.createConnection(callId, false);
        // TODO set call agent
        connection.setMode(mode);
        // TODO provide local connection options
        String localDescription = connection.halfOpen(new LocalConnectionOptions());
        this.responseParameters.put(MgcpParameterType.SDP, localDescription);
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
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, String remoteDescription, MgcpEndpoint endpoint) throws MgcpConnectionException {
        MgcpConnection connection = endpoint.createConnection(callId, false);
        // TODO set call agent
        String localDescription = connection.open(remoteDescription);
        this.responseParameters.put(MgcpParameterType.SDP, localDescription);
        connection.setMode(mode);
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
    private MgcpConnection createLocalConnection(int callId, ConnectionMode mode, MgcpEndpoint endpoint) throws MgcpConnectionException {
        MgcpConnection connection = endpoint.createConnection(callId, true);
        connection.open(null);
        connection.setMode(mode);
        return connection;
    }

    private void validateParameters() throws MgcpCommandException, RuntimeException {
        // Call ID
        Optional<Integer> callId = this.requestParameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (!callId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), MgcpResponseCode.INCORRECT_CALL_ID.message());
        }

        // Endpoint Name
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        if (endpointId.get().indexOf(WILDCARD_ALL) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }

        // Second Endpoint Name
        Optional<String> secondEndpointId = this.requestParameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        if (secondEndpointId.isPresent()) {
            if (secondEndpointId.get().indexOf(WILDCARD_ALL) != -1) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
            }
        }

        // Remote Description
        Optional<String> remoteSdp = this.requestParameters.getString(MgcpParameterType.SDP);
        if (secondEndpointId.isPresent() && remoteSdp.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Z2 and SDP present in message");
        }

        // Connection Mode
        Optional<String> mode = this.requestParameters.getString(MgcpParameterType.MODE);
        try {
            if (!mode.isPresent() || ConnectionMode.fromDescription(mode.get()) == null) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.message());
            }
        } catch (IllegalArgumentException e) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.message());
        }
    }

    private void executeCommand() throws MgcpConnectionException, MgcpCommandException {
        // Retrieve Endpoints
        Optional<String> endpointId1 = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        Optional<String> endpointId2 = this.requestParameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        MgcpEndpoint endpoint1 = retrieveEndpoint(endpointId1.get());
        MgcpEndpoint endpoint2 = endpointId2.isPresent() ? retrieveEndpoint(endpointId2.get()) : null;

        // Create Connections
        int callId = this.requestParameters.getIntegerBase16(MgcpParameterType.CALL_ID).get();
        ConnectionMode mode = ConnectionMode.fromDescription(this.requestParameters.getString(MgcpParameterType.MODE).get());
        
        if (endpoint2 == null) {
            final Optional<String> remoteDescription = this.requestParameters.getString(MgcpParameterType.SDP);
            
            MgcpConnection connection;
            if (!remoteDescription.isPresent()) {
                // Create half-open connection
                connection = createRemoteConnection(callId, mode, endpoint1);
            } else {
                // Create open connection
                connection = createRemoteConnection(callId, mode, remoteDescription.get(), endpoint1);
            }
            
            // Add parameters to response
            // XXX do not hardcode the endpoint address
            this.responseParameters.put(MgcpParameterType.ENDPOINT_ID, endpoint1.getEndpointId() + "@127.0.0.1:2427");
            this.responseParameters.put(MgcpParameterType.CONNECTION_ID, connection.getHexIdentifier());
        } else {
            // Create two local connections between both endpoints
            MgcpConnection connection1 = createLocalConnection(callId, mode, endpoint1);
            MgcpConnection connection2 = createLocalConnection(callId, ConnectionMode.SEND_RECV, endpoint2);
            
            // Add parameters to response
            // XXX do not hardcode the endpoint address
            this.responseParameters.put(MgcpParameterType.ENDPOINT_ID, endpoint1.getEndpointId() + "@127.0.0.1:2427");
            this.responseParameters.put(MgcpParameterType.CONNECTION_ID, connection1.getHexIdentifier());
            // XXX do not hardcode the endpoint address
            this.responseParameters.put(MgcpParameterType.SECOND_ENDPOINT, endpoint2.getEndpointId() + "@127.0.0.1:2427");
            this.responseParameters.put(MgcpParameterType.CONNECTION_ID2, connection2.getHexIdentifier());

            // Join connections
            ((MgcpLocalConnection) connection1).join((MgcpLocalConnection) connection2);
        }
    }

    private MgcpEndpoint retrieveEndpoint(String endpointId) throws MgcpCommandException {
        // Get local name
        final int indexOfSeparator = endpointId.indexOf(ENDPOINT_ID_SEPARATOR);
        final String localName = endpointId.substring(0, indexOfSeparator);

        final MgcpEndpoint endpoint;
        final int indexOfAll = endpointId.indexOf(WILDCARD_ANY);
        if (indexOfAll == -1) {
            // Search for registered endpoint
            endpoint = this.endpointManager.getEndpoint(localName);

            if (endpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.message());
            }
        } else {
            // Create new endpoint for a specific name space
            try {
                endpoint = this.endpointManager.registerEndpoint(localName.substring(0, indexOfAll));
            } catch (UnrecognizedMgcpNamespaceException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), e.getMessage());
            }
        }
        return endpoint;
    }

    @Override
    protected void execute() throws MgcpCommandException {
        try {
            validateParameters();
            executeCommand();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException | MgcpConnectionException e) {
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Could not process request");
        }
    }

    @Override
    protected void rollback() {
        // Retrieve Endpoints
        Optional<Integer> callId = this.requestParameters.getInteger(MgcpParameterType.CALL_ID);
        Optional<String> endpointId1 = this.responseParameters.getString(MgcpParameterType.ENDPOINT_ID);
        Optional<String> endpointId2 = this.responseParameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        Optional<Integer> connectionId1 = this.responseParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        Optional<Integer> connectionId2 = this.responseParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID2);

        MgcpEndpoint endpoint1 = endpointId1.isPresent() ? this.endpointManager.getEndpoint(endpointId1.get()) : null;
        MgcpEndpoint endpoint2 = endpointId2.isPresent() ? this.endpointManager.getEndpoint(endpointId2.get()) : null;

        // Delete created endpoints
        if (endpoint1 != null && connectionId1.isPresent()) {
            try {
                endpoint1.deleteConnection(callId.get(), connectionId1.get());
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.error("Could not delete primary connection. " + e.getMessage());
            }
        }

        if (endpoint2 != null && connectionId2.isPresent()) {
            try {
                endpoint2.deleteConnection(callId.get(), connectionId2.get());
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.error("Could not delete secondary connection. " + e.getMessage());
            }
        }
    }

}
