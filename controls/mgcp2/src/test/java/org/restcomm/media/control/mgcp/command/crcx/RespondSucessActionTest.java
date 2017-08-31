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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.local.MgcpLocalConnectionImpl;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RespondSucessActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRespondSuccess() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters requestParameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, requestParameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final String localSdp = "local-sdp-mock";
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final int primaryConnectionId = 5;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);

        final int secondaryConnectionId = 8;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);

        context.setCallback(callback);
        context.setPrimaryConnection(primaryConnection);
        context.setPrimaryEndpoint(primaryEndpoint);
        context.setSecondaryConnection(secondaryConnection);
        context.setSecondaryEndpoint(secondaryEndpoint);
        context.setLocalDescription(localSdp);

        // when
        final RespondSuccessAction action = new RespondSuccessAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.ABORT, context, stateMachine);

        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback).onSuccess(captor.capture());
        
        final MgcpCommandResult result = captor.getValue();
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(primaryEndpointId.toString(), responseParameters.getString(MgcpParameterType.ENDPOINT_ID).get());
        assertEquals(primaryConnection.getHexIdentifier(), responseParameters.getString(MgcpParameterType.CONNECTION_ID).get());
        assertEquals(secondaryEndpointId.toString(), responseParameters.getString(MgcpParameterType.SECOND_ENDPOINT).get());
        assertEquals(secondaryConnection.getHexIdentifier(), responseParameters.getString(MgcpParameterType.CONNECTION_ID2).get());
        assertEquals(localSdp, responseParameters.getString(MgcpParameterType.SDP).get());
    }


}
