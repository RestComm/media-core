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

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterPrimaryConnectionActionTest {

    @Test
    public void testUnregisterPrimaryConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final int callId = 9;
        final int connectionId = 2;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.getCallIdentifier()).thenReturn(callId);

        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                UnregisterConnectionCallback callback = invocation.getArgumentAt(2, UnregisterConnectionCallback.class);
                callback.onSuccess(connection);
                return null;
            }

        }).when(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));

        context.setCallId(callId);
        context.setPrimaryEndpoint(endpoint);
        context.setPrimaryConnection(connection);

        // when
        UnregisterPrimaryConnectionAction action = new UnregisterPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_UNREGISTERED, context);
        assertNull(context.getError());
    }
    
    @Test
    public void testUnregisterPrimaryConnectionFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final int callId = 9;
        final int connectionId = 2;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.getCallIdentifier()).thenReturn(callId);
        
        final Exception error = new Exception("test purposes");
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                UnregisterConnectionCallback callback = invocation.getArgumentAt(2, UnregisterConnectionCallback.class);
                callback.onFailure(error);
                return null;
            }
            
        }).when(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        context.setCallId(callId);
        context.setPrimaryEndpoint(endpoint);
        context.setPrimaryConnection(connection);
        
        // when
        UnregisterPrimaryConnectionAction action = new UnregisterPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);
        
        // then
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_UNREGISTERED, context);
        assertNull(context.getError());
    }

}
