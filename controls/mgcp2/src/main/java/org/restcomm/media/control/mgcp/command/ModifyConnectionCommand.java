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

package org.restcomm.media.control.mgcp.command;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionException;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;

/**
 * This command is used to modify the characteristics of a gateway's "view" of a connection.<br>
 * This "view" of the call includes both the local connection descriptor as well as the remote connection descriptor.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(ModifyConnectionCommand.class);

    public ModifyConnectionCommand(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager) {
        super(transactionId, parameters, endpointManager);
    }

    private void validateParameters(Parameters<MgcpParameterType> parameters, MdcxContext context) throws MgcpCommandException, RuntimeException {
        // Call ID
        Optional<Integer> callId = parameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (!callId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID);
        } else {
            context.callId = callId.get();
        }

        // Endpoint ID
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        } else if (endpointId.get().contains(WILDCARD_ALL) || endpointId.get().contains(WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
        } else {
            context.endpointId = endpointId.get();
        }

        // TODO Local Connection Options

        // Connection ID
        Optional<Integer> connectionId = parameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        if(!connectionId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID);
        } else {
            context.connectionId = connectionId.get();
        }

        // Connection Mode (optional)
        Optional<String> mode = parameters.getString(MgcpParameterType.MODE);
        if (mode.isPresent()) {
            try {
                ConnectionMode connectionMode = ConnectionMode.fromDescription(mode.get());
                context.mode = connectionMode;
            } catch (IllegalArgumentException e) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE);
            }
        }
        
        // Remote Description (optional)
        context.remoteDescription = parameters.getString(MgcpParameterType.SDP).or("");
    }

    private void executeCommand(MdcxContext context) throws MgcpCommandException {
        // Retrieve endpoint
        String endpointId = context.endpointId;
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(endpointId);

        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }

        // Retrieve connection from endpoint
        int callId = context.callId;
        int connectionId = context.connectionId;

        MgcpConnection connection = endpoint.getConnection(callId, connectionId);
        if (connection == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID);
        }

        // Set Mode (if specified)
        ConnectionMode mode = context.mode;
        if (mode != null) {
            connection.updateMode(mode);
        }

        // Set Remote Description (if defined)
        String remoteSdp = context.remoteDescription;
        if (!remoteSdp.isEmpty()) {
            try {
                MgcpConnectionState state = connection.getState();
                String localSdp = MgcpConnectionState.OPEN.equals(state) ? connection.renegotiate(remoteSdp) : connection.open(remoteSdp);
                context.localDescription = localSdp;
            } catch (MgcpConnectionException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNSUPPORTED_SDP);
            }
        }
    }
    
    private MgcpCommandResult respond(MdcxContext context) {
        Parameters<MgcpParameterType> parameters = new Parameters<>();
        MgcpCommandResult result = new MgcpCommandResult(this.transactionId, context.code, context.message, parameters);
        boolean successful = context.code < 300;
        
        if(successful) {
            translateContext(context, parameters);
        }
        return result;
    }
    
    private void translateContext(MdcxContext context, Parameters<MgcpParameterType> parameters) {
        // Local Description (optional)
        String localDescription = context.localDescription;
        if(!localDescription.isEmpty()) {
            parameters.put(MgcpParameterType.SDP, localDescription);
        }
    }

    @Override
    public void execute(FutureCallback<MgcpCommandResult> callback) {
        // Initialize empty context
        MdcxContext context = new MdcxContext();
        try {
            // Validate Parameters
            validateParameters(this.requestParameters, context);
            // Execute Command
            executeCommand(context);
            context.code = MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code();
            context.message = MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message();
        } catch (RuntimeException e) {
            log.error("Unexpected error occurred during tx=" + this.transactionId + " execution. Rolling back.");
            context.code = MgcpResponseCode.PROTOCOL_ERROR.code();
            context.message = MgcpResponseCode.PROTOCOL_ERROR.message();
        } catch (MgcpCommandException e) {
            log.error("Protocol error occurred during tx=" + this.transactionId + " execution: " + e.getMessage());
            context.code = e.getCode();
            context.message = e.getMessage();
        }
        MgcpCommandResult result = respond(context);
        callback.onSuccess(result);
    }

    private class MdcxContext {

        private int callId;
        private String endpointId;
        private int connectionId;
        private ConnectionMode mode;
        private String remoteDescription;
        private String localDescription;
        
        private int code;
        private String message;

        public MdcxContext() {
            this.callId = -1;
            this.endpointId = "";
            this.connectionId = -1;
            this.remoteDescription = "";
            this.localDescription = "";
            this.mode = null;

            this.code = MgcpResponseCode.ABORTED.code();
            this.message = MgcpResponseCode.ABORTED.message();
        }
    }

}
