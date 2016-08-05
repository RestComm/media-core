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
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;

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

    private void validateParameters() throws MgcpCommandException, RuntimeException {
        // Call ID
        Optional<Integer> callId = this.requestParameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        if (!callId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), MgcpResponseCode.INCORRECT_CALL_ID.message());
        }

        // Endpoint ID
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        } else if (endpointId.get().contains(WILDCARD_ALL) || endpointId.get().contains(WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
    }

    private void executeCommand() throws MgcpCommandException, MgcpCallNotFoundException, MgcpConnectionNotFound {
        // Retrieve endpoint
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(endpointId.get());
        
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        // Decide whether delete single or multiple connections
        Optional<Integer> callId = this.requestParameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        Optional<Integer> connectionId = this.requestParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        
        if (connectionId.isPresent()) {
            // Delete specific connection
            MgcpConnection deleted = endpoint.deleteConnection(callId.get(), connectionId.get());
            // TODO Gather statistics from connection
            this.responseParameters.put(MgcpParameterType.CONNECTION_PARAMETERS, "PS=" + 0 + ", PR=" + 0);
        } else {
            // Bulk delete connections
            try {
                endpoint.deleteConnections(callId.get());
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

    @Override
    protected void execute() throws MgcpCommandException {
        try {
            validateParameters();
            executeCommand();
        } catch (MgcpConnectionNotFound e) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        } catch (MgcpCallNotFoundException e) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), MgcpResponseCode.INCORRECT_CALL_ID.message());
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    @Override
    protected void rollback() {
        // Nothing to cleanup
    }

}
