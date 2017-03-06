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
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.base.Optional;

/**
 * This command is used to terminate a single connection or multiple connections at the same time.<br>
 * As a side effect, it collects statistics on the execution of the connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(DeleteConnectionCommand.class);

    public DeleteConnectionCommand(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager) {
        super(transactionId, parameters, endpointManager);
    }

    private void validateParameters(Parameters<MgcpParameterType> parameters, DlcxContext context) throws MgcpCommandException, RuntimeException {
        // Endpoint ID
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        } else if (endpointId.get().contains(WILDCARD_ALL) || endpointId.get().contains(WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        } else {
            context.endpointId = endpointId.get();
        }

        // Call ID (optional)
        Optional<Integer> callId = parameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (callId.isPresent()) {
            context.callId = callId.get();
        }
        
        // Connection ID (optional)
        Optional<Integer> connectionId = this.requestParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        if(connectionId.isPresent()) {
            context.connectionId = connectionId.get();
            
            // Call ID is mandatory in this case
            if(!callId.isPresent()) {
                throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID);
            }
        }
    }

    private void executeCommand(DlcxContext context) throws MgcpCommandException, MgcpCallNotFoundException, MgcpConnectionNotFoundException {
        // Retrieve endpoint
        String endpointId = context.endpointId;
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(endpointId);
        
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }

        // Decide whether delete single or multiple connections
        int callId = context.callId;
        int connectionId = context.connectionId;
        
        if(connectionId == -1) {
            // Delete multiple endpoints...
            if(callId == -1) {
                // ... all connections in the endpoint
                endpoint.deleteConnections();
            } else {
                // ... all connections involved in a particular call
                try {
                    endpoint.deleteConnections(callId);
                } catch (MgcpCallNotFoundException e) {
                    /*
                     * https://tools.ietf.org/html/rfc3435#section-2.3.9
                     * 
                     * Note that the command will still succeed if there were no connections with the CallId specified, as long as
                     * the EndpointId was valid.
                     */
                }
            }
        } else {
            // Delete single connection bound to a specific call
            MgcpConnection deleted = endpoint.deleteConnection(callId, connectionId);
            // TODO Gather statistics from connection
            context.connectionParams = "PS=" + 0 + ", PR=" + 0;
        }
    }
    
    private MgcpCommandResult respond(DlcxContext context) {
        Parameters<MgcpParameterType> parameters = new Parameters<>();
        MgcpCommandResult result = new MgcpCommandResult(this.transactionId, context.code, context.message, parameters);
        boolean successful = context.code < 300;
        
        if(successful) {
            translateContext(context, parameters);
        }
        return result;
    }
    
    private void translateContext(DlcxContext context, Parameters<MgcpParameterType> parameters) {
       // Primary endpoint and connection
       final String connectionParams = context.connectionParams;
       if(!connectionParams.isEmpty()) {
           parameters.put(MgcpParameterType.CONNECTION_PARAMETERS, connectionParams);
       }
    }

    @Override
    public MgcpCommandResult call() {
        // Initialize empty context
        DlcxContext context = new DlcxContext();
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
        } catch (MgcpCallNotFoundException e) {
            log.error("Protocol error occurred during tx=" + this.transactionId + " execution: " + e.getMessage());
            context.code = MgcpResponseCode.INCORRECT_CALL_ID.code();
            context.message = MgcpResponseCode.INCORRECT_CALL_ID.message();
        } catch (MgcpConnectionNotFoundException e) {
            log.error("Protocol error occurred during tx=" + this.transactionId + " execution: " + e.getMessage());
            context.code = MgcpResponseCode.INCORRECT_CONNECTION_ID.code();
            context.message = MgcpResponseCode.INCORRECT_CONNECTION_ID.message();
        }  catch (MgcpCommandException e) {
            log.error("Protocol error occurred during tx=" + this.transactionId + " execution: " + e.getMessage());
            context.code = e.getCode();
            context.message = e.getMessage();
        }
        // Build response
        return respond(context);
    }

    private class DlcxContext {
        
        private int callId;
        private String endpointId;
        private int connectionId;
        private String connectionParams;
        
        private int code;
        private String message;
        
        public DlcxContext() {
            this.callId = -1;
            this.endpointId = "";
            this.connectionId = -1;
            this.connectionParams = "";
            
            this.code = MgcpResponseCode.ABORTED.code();
            this.message = MgcpResponseCode.ABORTED.message();
        }
    }

}
