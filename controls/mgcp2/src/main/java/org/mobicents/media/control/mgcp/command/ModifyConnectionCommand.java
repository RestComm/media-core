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
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;
import org.mobicents.media.server.spi.ConnectionMode;

import com.google.common.base.Optional;

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

        // TODO Local Connection Options

        // Connection ID
        Optional<Integer> connectionId = this.requestParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);
        if(!connectionId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        }

        // Connection Mode
        Optional<String> mode = this.requestParameters.getString(MgcpParameterType.MODE);
        if (!mode.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.message());
        } else {
            try {
                ConnectionMode connectionMode = ConnectionMode.fromDescription(mode.get());
            } catch (IllegalArgumentException e) {
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.message());
            }
        }
    }

    private void executeCommand() throws MgcpCommandException {
        // Retrieve endpoint
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(endpointId.get());

        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        // Retrieve connection from endpoint
        Optional<Integer> callId = this.requestParameters.getIntegerBase16(MgcpParameterType.CALL_ID);
        Optional<Integer> connectionId = this.requestParameters.getIntegerBase16(MgcpParameterType.CONNECTION_ID);

        MgcpConnection connection = endpoint.getConnection(callId.get(), connectionId.get());
        if (connection == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), MgcpResponseCode.INCORRECT_CONNECTION_ID.message());
        }

        // Set Mode (if specified)
        Optional<String> mode = this.requestParameters.getString(MgcpParameterType.MODE);
        if (mode.isPresent()) {
            connection.setMode(ConnectionMode.fromDescription(mode.get()));
        }

        // Set Remote Description (if defined)
        Optional<String> remoteSdp = this.requestParameters.getString(MgcpParameterType.SDP);
        if (remoteSdp.isPresent()) {
            try {
                String localSdp = connection.open(remoteSdp.get());
                this.responseParameters.put(MgcpParameterType.SDP, localSdp);
            } catch (MgcpConnectionException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNSUPPORTED_SDP.code(), MgcpResponseCode.UNSUPPORTED_SDP.message());
            }
        }
    }

    @Override
    protected void execute() throws MgcpCommandException {
        try {
            validateParameters();
            executeCommand();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    @Override
    protected void rollback() {
        // Nothing to cleanup
    }

}
