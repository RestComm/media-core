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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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
public class RegisterPrimaryConnectionActionTest {

    @Test
    public void testOpenPrimaryConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpConnection connection = mock(MgcpConnection.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                RegisterConnectionCallback callback = invocation.getArgumentAt(1, RegisterConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(endpoint).registerConnection(eq(connection), any(RegisterConnectionCallback.class));

        context.setPrimaryEndpoint(endpoint);
        context.setPrimaryConnection(connection);

        // when
        RegisterPrimaryConnectionAction action = new RegisterPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.ABORT, context, stateMachine);

        // then
        verify(endpoint).registerConnection(eq(connection), any(RegisterConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_REGISTERED, context);
        assertNull(context.getError());
    }
    
    @Test
    public void testOpenPrimaryConnectionFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final Exception error = new Exception("test purposes");
        final MgcpConnection connection = mock(MgcpConnection.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                RegisterConnectionCallback callback = invocation.getArgumentAt(1, RegisterConnectionCallback.class);
                callback.onFailure(error);
                return null;
            }
            
        }).when(endpoint).registerConnection(eq(connection), any(RegisterConnectionCallback.class));
        
        context.setPrimaryEndpoint(endpoint);
        context.setPrimaryConnection(connection);
        
        // when
        RegisterPrimaryConnectionAction action = new RegisterPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.ABORT, context, stateMachine);
        
        // then
        verify(endpoint).registerConnection(eq(connection), any(RegisterConnectionCallback.class));
        verify(stateMachine).fireImmediate(CreateConnectionEvent.ABORT, context);
        assertEquals(error, context.getError());
    }

}
