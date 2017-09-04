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

import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Action that sends successful notification about MGCP Command execution.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RespondSuccessAction
        extends AnonymousAction<CreateConnectionFsm, CreateConnectionState, CreateConnectionEvent, CreateConnectionContext>
        implements CreateConnectionAction {
    
    static final RespondSuccessAction INSTANCE = new RespondSuccessAction();
    
    RespondSuccessAction() {
        super();
    }

    @Override
    public void execute(CreateConnectionState from, CreateConnectionState to, CreateConnectionEvent event, CreateConnectionContext context, CreateConnectionFsm stateMachine) {
        final int transactionId = context.getTransactionId();
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpResponseCode response = MgcpResponseCode.TRANSACTION_WAS_EXECUTED;
        
        // Build the result
        MgcpCommandResult result = new MgcpCommandResult(transactionId, response.code(), response.message(), parameters);
        translateContext(context, parameters);
        
        // Send the result to the callback
        FutureCallback<MgcpCommandResult> callback = context.getCallback();
        callback.onSuccess(result);
    }

    private void translateContext(CreateConnectionContext context, Parameters<MgcpParameterType> parameters) {
        // Primary endpoint and connection
        final MgcpEndpoint primaryEndpoint = context.getPrimaryEndpoint();
        final MgcpConnection primaryConnection = context.getPrimaryConnection();

        if (primaryEndpoint != null && primaryConnection != null) {
            parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpoint.getEndpointId().toString());
            parameters.put(MgcpParameterType.CONNECTION_ID, primaryConnection.getHexIdentifier());
        }

        // Secondary endpoint and connection
        final MgcpEndpoint secondaryEndpoint = context.getSecondaryEndpoint();
        final MgcpConnection secondaryConnection = context.getSecondaryConnection();

        if (secondaryEndpoint != null && secondaryConnection != null) {
            parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpoint.getEndpointId().toString());
            parameters.put(MgcpParameterType.CONNECTION_ID2, secondaryConnection.getHexIdentifier());
        }

        // Local Session Description
        final String localDescription = context.getLocalDescription();

        if (!localDescription.isEmpty()) {
            parameters.put(MgcpParameterType.SDP, localDescription);
        }
    }

}
