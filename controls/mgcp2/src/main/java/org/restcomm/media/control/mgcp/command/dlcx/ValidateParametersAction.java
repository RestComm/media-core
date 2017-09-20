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

package org.restcomm.media.control.mgcp.command.dlcx;

import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.base.Optional;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ValidateParametersAction
        extends AnonymousAction<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> {

    @Override
    public void execute(DeleteConnectionState from, DeleteConnectionState to, DeleteConnectionEvent event,
            DeleteConnectionContext context, DeleteConnectionFsm stateMachine) {
        final MgcpEndpointManager endpointManager = context.getEndpointManager();
        final Parameters<MgcpParameterType> parameters = context.getParameters();
        
        try {
            String endpointId = loadEndpointId(parameters);
            MgcpEndpoint endpoint = loadEndpoint(endpointId, endpointManager);
            int callId = loadCallId(parameters);
            int connectionId = loadConnectionId(parameters);
            
            context.setEndpointId(endpointId);
            context.setEndpoint(endpoint);
            context.setCallId(callId);
            context.setConnectionId(connectionId);
            
            stateMachine.fire(DeleteConnectionEvent.VALIDATED_PARAMETERS, context);
        } catch (MgcpCommandException e) {
            context.setError(e);
            stateMachine.fire(DeleteConnectionEvent.FAILURE, context);
        }
    }

    private String loadEndpointId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        String endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID).or("");
        if (endpointId.isEmpty()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        } else if (endpointId.contains(MgcpCommand.WILDCARD_ALL) || endpointId.contains(MgcpCommand.WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
        return endpointId;
    }

    private int loadCallId(Parameters<MgcpParameterType> parameters) {
        return parameters.getIntegerBase16(MgcpParameterType.CALL_ID).or(DeleteConnectionContext.NO_CALL_ID).intValue();
    }

    private int loadConnectionId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<Integer> connectionId = parameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        if (connectionId.isPresent()) {
            // Call ID is mandatory in this case
            Optional<Integer> callId = parameters.getIntegerBase16(MgcpParameterType.CALL_ID);
            if (!callId.isPresent()) {
                throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID);
            }
        }
        return connectionId.or(DeleteConnectionContext.NO_CONNECTION_ID).intValue();
    }
    
    private MgcpEndpoint loadEndpoint(String endpointId, MgcpEndpointManager endpointManager) throws MgcpCommandException {
        MgcpEndpoint endpoint = endpointManager.getEndpoint(endpointId);
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }
        return endpoint;
    }

}
