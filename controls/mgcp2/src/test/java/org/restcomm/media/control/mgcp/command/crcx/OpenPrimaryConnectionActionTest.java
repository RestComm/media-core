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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class OpenPrimaryConnectionActionTest {

    @Test
    public void testOpenPrimaryConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final String remoteSdp = "remote-sdp-mock";
        final String localSdp = "local-sdp-mock";
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpConnection connection = mock(MgcpConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OpenConnectionCallback callback = invocation.getArgumentAt(1, OpenConnectionCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }

        }).when(connection).open(eq(remoteSdp), any(OpenConnectionCallback.class));

        context.setPrimaryConnection(connection);
        context.setRemoteDescription(remoteSdp);

        // when
        OpenPrimaryConnectionAction action = new OpenPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        verify(connection).open(eq(remoteSdp), any(OpenConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_OPENED, context);
        assertEquals(localSdp, context.getLocalDescription());
        assertNull(context.getError());
    }
    
    @Test
    public void testOpenPrimaryConnectionFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final String remoteSdp = "remote-sdp-mock";
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final Exception error = new Exception("test purposes");
        final MgcpConnection connection = mock(MgcpConnection.class);
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OpenConnectionCallback callback = invocation.getArgumentAt(1, OpenConnectionCallback.class);
                callback.onFailure(error);
                return null;
            }
            
        }).when(connection).open(eq(remoteSdp), any(OpenConnectionCallback.class));
        
        context.setPrimaryConnection(connection);
        context.setRemoteDescription(remoteSdp);
        
        // when
        OpenPrimaryConnectionAction action = new OpenPrimaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);
        
        // then
        verify(connection).open(eq(remoteSdp), any(OpenConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.FAILURE, context);
        assertEquals("", context.getLocalDescription());
        assertEquals(error, context.getError());
    }

}
