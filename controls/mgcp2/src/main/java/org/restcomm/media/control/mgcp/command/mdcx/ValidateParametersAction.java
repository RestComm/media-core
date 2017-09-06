/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.command.mdcx;

import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.base.Optional;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class ValidateParametersAction extends ModifyConnectionAction {

    static final ValidateParametersAction INSTANCE = new ValidateParametersAction();

    ValidateParametersAction() {
        super();
    }

    @Override
    public void execute(ModifyConnectionState from, ModifyConnectionState to, ModifyConnectionEvent event,
            ModifyConnectionContext context, ModifyConnectionFsm stateMachine) {
        final MgcpCommandParameters parameters = context.getParameters();

        try {
            // Load parameters and validate them
            int callId = loadCallId(parameters);
            int connectionId = loadConnectionId(parameters);
            String endpointId = loadEndpointId(parameters);
            ConnectionMode mode = loadMode(parameters);
            String remoteDescription = loadRemoteDescription(parameters);
            // TODO Local Connection Options
            
            // Validate endpoint exists
            MgcpEndpoint endpoint = loadEndpoint(endpointId, context.getEndpointManager());
            MgcpConnection connection = loadConnection(callId, connectionId, endpoint);
            
            // Update context
            context.setCallId(callId);
            context.setConnectionId(connectionId);
            context.setConnection(connection);
            context.setEndpointId(endpointId);
            context.setEndpoint(endpoint);
            context.setMode(mode);
            context.setRemoteDescription(remoteDescription);
            
            // Parameters loaded successfully. Move to next state.
            stateMachine.fire(ModifyConnectionEvent.VALIDATED_PARAMETERS, context);
        } catch (MgcpCommandException e) {
            // Save error in context and move to FAILED state
            context.setError(e);
            stateMachine.fire(ModifyConnectionEvent.FAILURE, context);
        }
    }

    private int loadCallId(MgcpCommandParameters parameters) throws MgcpCommandException {
        Optional<Integer> callId = parameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (callId.isPresent()) {
            return callId.get().intValue();
        } else {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID);
        }
    }

    private String loadEndpointId(MgcpCommandParameters parameters) throws MgcpCommandException {
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (endpointId.isPresent() && !endpointId.get().isEmpty()) {
            if (endpointId.get().contains(MgcpCommand.WILDCARD_ALL) || endpointId.get().contains(MgcpCommand.WILDCARD_ANY)) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
            } else {
                return endpointId.get();
            }
        } else {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }
    }

    private int loadConnectionId(MgcpCommandParameters parameters) throws MgcpCommandException {
        Optional<Integer> connectionId = parameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        
        if(connectionId.isPresent()) {
            return connectionId.get().intValue();
        } else {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID);
        }
    }
    
    private ConnectionMode loadMode(MgcpCommandParameters parameters) throws MgcpCommandException {
        String mode = parameters.getString(MgcpParameterType.MODE).or("");
        
        if(!mode.isEmpty()) {
            try {
                ConnectionMode connectionMode = ConnectionMode.fromDescription(mode);
                return connectionMode;
            } catch (IllegalArgumentException e) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE);
            }
        } else {
            return null;
        }
    }
    
    private String loadRemoteDescription(MgcpCommandParameters parameters) {
        return parameters.getString(MgcpParameterType.SDP).or("");
    }
    
    private MgcpEndpoint loadEndpoint(String endpointId, MgcpEndpointManager endpoints) throws MgcpCommandException {
        final MgcpEndpoint endpoint = endpoints.getEndpoint(endpointId);
        if(endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }
        return endpoint;
    }

    private MgcpConnection loadConnection(int callId, int connectionId, MgcpEndpoint endpoint) throws MgcpCommandException {
        final MgcpConnection connection = endpoint.getConnection(callId, connectionId);
        if(connection == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID);
        }
        return connection;
    }

}
