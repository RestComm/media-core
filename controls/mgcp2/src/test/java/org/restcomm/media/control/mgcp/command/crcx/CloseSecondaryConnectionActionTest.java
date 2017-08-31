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
public class CloseSecondaryConnectionActionTest {

    @Test
    public void testCloseSecondaryConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpConnection connection = mock(MgcpConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(connection).close(any(CloseConnectionCallback.class));

        context.setSecondaryConnection(connection);

        // when
        CloseSecondaryConnectionAction action = new CloseSecondaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        verify(connection, timeout(50)).close(any(CloseConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_CLOSED, context);
        assertNull(context.getError());
    }

    @Test
    public void testCloseSecondaryConnectionFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final Exception error = new Exception("testing purposes");
        final MgcpConnection connection = mock(MgcpConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onFailure(error);
                return null;
            }

        }).when(connection).close(any(CloseConnectionCallback.class));

        context.setSecondaryConnection(connection);

        // when
        CloseSecondaryConnectionAction action = new CloseSecondaryConnectionAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        verify(connection, timeout(50)).close(any(CloseConnectionCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.FAILURE, context);
        assertEquals(error, context.getError());
    }

}
