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

package org.restcomm.media.control.mgcp.command.crcx;

import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptionsParser;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.restcomm.media.spi.ConnectionMode;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.base.Optional;

/**
 * Parses and validates the parameters passed to the CRCX Command.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class ValidateParametersAction extends
        AnonymousAction<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext>
        implements CreateConnectionAction {
    
    static final ValidateParametersAction INSTANCE = new ValidateParametersAction();
    
    ValidateParametersAction() {
        super();
    }

    @Override
    public void execute(CreateConnectionState from, CreateConnectionState to, CreateConnectionEvent event, CreateConnectionContext context, CreateConnectionFsm stateMachine) {
        try {
            // Parse and retrieve parameters
            final Parameters<MgcpParameterType> parameters = context.getParameters();
            int callId = loadCallId(parameters);
            String primaryEndpointId = loadEndpointId(parameters);
            String secondaryEndpointId = loadSecondEndpointId(parameters);
            String remoteDescription = loadRemoteDescription(parameters);
            ConnectionMode connectionMode = loadConnectionMode(parameters);
            LocalConnectionOptions lcOptions = loadLocalConnectionOptions(parameters);
            
            // Verify existence of required resources
            final MgcpEndpointManager endpointManager = context.getEndpointManager();
            MgcpEndpoint primaryEndpoint = retrieveEndpoint(primaryEndpointId, endpointManager);
            MgcpEndpoint secondaryEndpoint = secondaryEndpointId.isEmpty() ? null : retrieveEndpoint(secondaryEndpointId, endpointManager) ;

            // Persist parameters into global context
            context.setCallId(callId);
            context.setPrimaryEndpointId(primaryEndpointId);
            context.setPrimaryEndpoint(primaryEndpoint);
            context.setSecondaryEndpointId(secondaryEndpointId);
            context.setSecondaryEndpoint(secondaryEndpoint);
            context.setRemoteDescription(remoteDescription);
            context.setConnectionMode(connectionMode);
            context.setLocalConnectionOptions(lcOptions);
            
            // Parameters have been validated successfully
            stateMachine.fire(CreateConnectionEvent.VALIDATED_PARAMETERS, context);
        } catch (MgcpCommandException e) {
            context.setError(e);
            stateMachine.fireImmediate(CreateConnectionEvent.ABORT, context);
        }
    }

    private int loadCallId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
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
        if (endpointId.get().indexOf(MgcpCommand.WILDCARD_ALL) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
        }
        return endpointId.get();
    }

    private String loadSecondEndpointId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> secondEndpointId = parameters.getString(MgcpParameterType.SECOND_ENDPOINT);
        if (secondEndpointId.isPresent()) {
            if (secondEndpointId.get().indexOf(MgcpCommand.WILDCARD_ALL) != -1) {
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

    private LocalConnectionOptions loadLocalConnectionOptions(Parameters<MgcpParameterType> parameters)
            throws MgcpCommandException {
        Optional<String> lcOptions = parameters.getString(MgcpParameterType.LOCAL_CONNECTION_OPTIONS);
        if (!lcOptions.isPresent()) {
            return new LocalConnectionOptions();
        }
        try {
            return LocalConnectionOptionsParser.INSTANCE.parse(lcOptions.get());
        } catch (MgcpParseException e) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Could not decode Local Connection Options");
        }
    }
    
    private MgcpEndpoint retrieveEndpoint(String endpointId, MgcpEndpointManager endpointManager) throws MgcpCommandException {
        // Get local name
        final int indexOfSeparator = endpointId.indexOf(MgcpCommand.ENDPOINT_ID_SEPARATOR);
        final String localName = endpointId.substring(0, indexOfSeparator);

        final MgcpEndpoint endpoint;
        final int indexOfAll = endpointId.indexOf(MgcpCommand.WILDCARD_ANY);
        if (indexOfAll == -1) {
            // Search for registered endpoint
            endpoint = endpointManager.getEndpoint(endpointId);

            if (endpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
            }
        } else {
            // Create new endpoint for a specific name space
            try {
                endpoint = endpointManager.registerEndpoint(localName.substring(0, indexOfAll));
            } catch (UnrecognizedMgcpNamespaceException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE);
            }
        }
        return endpoint;
    }
}
