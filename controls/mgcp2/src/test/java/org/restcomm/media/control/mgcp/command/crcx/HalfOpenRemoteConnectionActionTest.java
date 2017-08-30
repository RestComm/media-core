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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.MgcpRemoteConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class HalfOpenRemoteConnectionActionTest {

    @Test
    public void testHalfOpenRemoteConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 3;
        final String localSdp = "local-sdp-mock";
        final LocalConnectionOptions lcOptions = mock(LocalConnectionOptions.class);
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OpenConnectionCallback callback = invocation.getArgumentAt(1, OpenConnectionCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }

        }).when(connection).halfOpen(any(LocalConnectionOptions.class), any(OpenConnectionCallback.class));

        context.setCallId(callId);
        context.setPrimaryConnection(connection);
        context.setLocalConnectionOptions(lcOptions);

        // when
        HalfOpenPrimaryConnectionAction action = new HalfOpenPrimaryConnectionAction();
        action.execute(CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionState.EXECUTING, CreateConnectionEvent.VALIDATED_PARAMETERS, context, stateMachine);

        // then
        verify(connection, timeout(50)).halfOpen(eq(lcOptions), any(OpenConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_OPENED, context);
        assertEquals(connection, context.getPrimaryConnection());
        assertEquals(localSdp, context.getLocalDescription());
        assertNull(context.getError());
    }

    @Test
    public void testHalfOpenRemoteConnectionFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 3;
        final LocalConnectionOptions lcOptions = mock(LocalConnectionOptions.class);
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final Exception error = new Exception("testing purposes");
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                OpenConnectionCallback callback = invocation.getArgumentAt(1, OpenConnectionCallback.class);
                callback.onFailure(error);
                return null;
            }

        }).when(connection).halfOpen(any(LocalConnectionOptions.class), any(OpenConnectionCallback.class));

        context.setCallId(callId);
        context.setPrimaryConnection(connection);
        context.setLocalConnectionOptions(lcOptions);

        // when
        HalfOpenPrimaryConnectionAction action = new HalfOpenPrimaryConnectionAction();
        action.execute(CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionState.EXECUTING, CreateConnectionEvent.VALIDATED_PARAMETERS, context, stateMachine);

        // then
        verify(connection, timeout(50)).halfOpen(eq(lcOptions), any(OpenConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.ABORT, context);
        assertEquals(connection, context.getPrimaryConnection());
        assertEquals("", context.getLocalDescription());
        assertEquals(error, context.getError());
    }

}
