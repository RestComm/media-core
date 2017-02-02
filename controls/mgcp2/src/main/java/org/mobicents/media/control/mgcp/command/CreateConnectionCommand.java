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

    protected static final String WILDCARD_ALL = "*";
    protected static final String WILDCARD_ANY = "$";
    protected static final String ENDPOINT_ID_SEPARATOR = "@";
    
    public CreateConnectionCommand(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager) {
        super(transactionId, parameters, endpointManager);
    }

    private int loadCallId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        // Call ID
        Optional<Integer> callId = parameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (!callId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID);
        }
        return callId.get();
    }

    private String loadEndpointId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }
        if (endpointId.get().indexOf(WILDCARD_ALL) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
        }
        return endpointId.get();
    }

    private String loadSecondEndpointId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> secondEndpointId = parameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        if (secondEndpointId.isPresent()) {
            if (secondEndpointId.get().indexOf(WILDCARD_ALL) != -1) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
            }
        }
        return secondEndpointId.or("");
    }

    private String loadRemoteDescription(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> remoteSdp = parameters.getString(MgcpParameterType.SDP);
        Optional<String> secondEndpointId = parameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        if (secondEndpointId.isPresent() && remoteSdp.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Z2 and SDP present in message");
        }
        return remoteSdp.or("");
    }

    private ConnectionMode loadConnectionMode(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> mode = parameters.getString(MgcpParameterType.MODE);
        try {
            if (!mode.isPresent()) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE);
            } else {
                return ConnectionMode.fromDescription(mode.get());
            }
        } catch (IllegalArgumentException e) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE);
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
            endpoint = this.endpointManager.getEndpoint(endpointId);

            if (endpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
            }
        } else {
            // Create new endpoint for a specific name space
            try {
                endpoint = this.endpointManager.registerEndpoint(localName.substring(0, indexOfAll));
            } catch (UnrecognizedMgcpNamespaceException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE);
            }
        }
        return endpoint;
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
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, MgcpEndpoint endpoint, CrcxContext context) throws MgcpConnectionException {
        // Create connection
        MgcpConnection connection = endpoint.createConnection(callId, false);
        // TODO set call agent
        // TODO provide local connection options
        String localDescription = connection.halfOpen(new LocalConnectionOptions());
        context.setLocalDescription(localDescription);
        connection.setMode(mode);
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
    private MgcpConnection createRemoteConnection(int callId, ConnectionMode mode, String remoteDescription, MgcpEndpoint endpoint, CrcxContext context) throws MgcpConnectionException {
        MgcpConnection connection = endpoint.createConnection(callId, false);
        // TODO set call agent
        String localDescription = connection.open(remoteDescription);
        context.setLocalDescription(localDescription);

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
     * @param secondEndpoint The endpoint where the connection will be registered to.
     * 
     * @return The new connection
     * @throws MgcpException If connection could not be opened.
     */
    private MgcpConnection createLocalConnection(int callId, MgcpEndpoint endpoint) throws MgcpConnectionException {
        MgcpConnection connection = endpoint.createConnection(callId, true);
        connection.open(null);
        return connection;
    }

    private void validateParameters(Parameters<MgcpParameterType> parameters, CrcxContext context) throws MgcpCommandException {
        context.setCallId(loadCallId(parameters));
        context.setEndpointId(loadEndpointId(parameters));
        context.setSecondEndpointId(loadSecondEndpointId(parameters));
        context.setRemoteDescription(loadRemoteDescription(parameters));
        context.setConnectionMode(loadConnectionMode(parameters));
    }

    private void executeCommand(CrcxContext context) throws MgcpCommandException, MgcpConnectionException {
        // Retrieve Endpoints
        final String endpointId = context.getEndpointId();
        final String secondEndpointId = context.getSecondEndpointId();
        final MgcpEndpoint endpoint1 = retrieveEndpoint(endpointId);
        final MgcpEndpoint endpoint2 = secondEndpointId.isEmpty() ? null : retrieveEndpoint(secondEndpointId);
        
        // Update context with endpoint ID (in case they new endpoints were created)
        context.setEndpointId(endpoint1.getEndpointId().toString());
        if(endpoint2 != null) {
            context.setSecondEndpointId(endpoint2.getEndpointId().toString());
        }

        // Create Connections
        if (endpoint2 == null) {
            MgcpConnection connection;
            if (context.getRemoteDescription().isEmpty()) {
                // Create half-open connection
                connection = createRemoteConnection(context.getCallId(), context.getConnectionMode(), endpoint1, context);
            } else {
                // Create open connection
                connection = createRemoteConnection(context.getCallId(), context.getConnectionMode(), context.getRemoteDescription(), endpoint1, context);
            }
            
            // Update context with identifiers of newly created connection
            context.setConnectionId(connection.getIdentifier());
        } else {
            // Create two local connections between both endpoints
            MgcpConnection connection1 = createLocalConnection(context.getCallId(), endpoint1);
            MgcpConnection connection2 = createLocalConnection(context.getCallId(), endpoint2);
            
            // Update context with identifiers of newly created connection
            context.setConnectionId(connection1.getIdentifier());
            context.setSecondConnectionId(connection2.getIdentifier());

            // Join connections
            ((MgcpLocalConnection) connection1).join((MgcpLocalConnection) connection2);
            
            // Set connection mode
            connection1.setMode(context.getConnectionMode());
            connection2.setMode(ConnectionMode.SEND_RECV);
        }
    }
    
    private void rollback(CrcxContext context) {
        final int callId = context.getCallId();
        final String endpointId = context.getEndpointId();
        final String secondEndpointId = context.getSecondEndpointId();
        final int connectionId = context.getConnectionId();
        final int secondConnectionId = context.getSecondConnectionId();
        
        // Retrieve Endpoints
        MgcpEndpoint endpoint1 = endpointId.isEmpty() ? null : this.endpointManager.getEndpoint(endpointId);
        MgcpEndpoint endpoint2 = secondEndpointId.isEmpty() ? null : this.endpointManager.getEndpoint(secondEndpointId);

        // Delete created endpoints
        if (endpoint1 != null && connectionId > 0) {
            try {
                endpoint1.deleteConnection(callId, connectionId);
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.error("Could not delete primary connection. " + e.getMessage());
            }
        }

        if (endpoint2 != null && secondConnectionId > 0) {
            try {
                endpoint2.deleteConnection(callId, secondConnectionId);
            } catch (MgcpCallNotFoundException | MgcpConnectionNotFound e) {
                log.error("Could not delete secondary connection. " + e.getMessage());
            }
        }
        
    }
    
    private MgcpCommandResult respond(CrcxContext context) {
        Parameters<MgcpParameterType> parameters = new Parameters<>();
        MgcpCommandResult result = new MgcpCommandResult(this.transactionId, context.getCode(), context.getMessage(), parameters);
        boolean successful = context.getCode() < 300;
        
        if(successful) {
            translateContext(context, parameters);
        }
        return result;
    }
    
    private void translateContext(CrcxContext context, Parameters<MgcpParameterType> parameters) {
        // Primary endpoint and connection
        final String endpointId = context.getEndpointId();
        final int connectionId = context.getConnectionId();
        if (!endpointId.isEmpty() && connectionId > 0) {
            parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
            parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        }
        
       final String secondEndpointId = context.getSecondEndpointId();
       final int secondConnectionId = context.getSecondConnectionId();
       if(!secondEndpointId.isEmpty() && secondConnectionId > 0) {
           parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondEndpointId);
           parameters.put(MgcpParameterType.CONNECTION_ID2, Integer.toHexString(secondConnectionId));
       }
       
       final String localDescription = context.getLocalDescription();
       if(!localDescription.isEmpty()) {
           parameters.put(MgcpParameterType.SDP, localDescription);
       }
    }
    
    @Override
    public MgcpCommandResult call() {
        // Initialize empty context
        CrcxContext context = new CrcxContext();
        try {
            // Validate Parameters
            validateParameters(this.requestParameters, context);
            // Execute Command
            executeCommand(context);
            context.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
            context.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        } catch (RuntimeException | MgcpConnectionException e) {
            log.error("Unexpected error occurred during tx=" + this.transactionId + " execution. Reason: " + e.getMessage() + ". Rolling back.");
            rollback(context);
            context.setCode(MgcpResponseCode.PROTOCOL_ERROR.code());
            context.setMessage(MgcpResponseCode.PROTOCOL_ERROR.message());
        } catch (MgcpCommandException e) {
            log.error("Protocol error occurred during tx=" + this.transactionId + " execution. Reason: " + e.getMessage());
            context.setCode(e.getCode());
            context.setMessage(e.getMessage());
        }
        return respond(context);
    }
    

}
