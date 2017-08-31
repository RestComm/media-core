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
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.MgcpLocalConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class JoinConnectionsActionTest {

    @Test
    public void testJoinConnections() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpLocalConnection primaryConnection = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection secondaryConnection = mock(MgcpLocalConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                JoinConnectionsCallback callback = invocation.getArgumentAt(1, JoinConnectionsCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(primaryConnection).join(eq(secondaryConnection), any(JoinConnectionsCallback.class));

        context.setPrimaryConnection(primaryConnection);
        context.setSecondaryConnection(secondaryConnection);

        // when
        JoinConnectionsAction action = new JoinConnectionsAction();
        action.execute(CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionState.EXECUTING, CreateConnectionEvent.VALIDATED_PARAMETERS, context, stateMachine);

        // then
        verify(primaryConnection).join(eq(secondaryConnection), any(JoinConnectionsCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTIONS_JOINED, context);
        assertNull(context.getError());
    }

    @Test
    public void testJoinConnectionsFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId,
                parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final Exception error = new Exception("test purposes");
        final MgcpLocalConnection primaryConnection = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection secondaryConnection = mock(MgcpLocalConnection.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                JoinConnectionsCallback callback = invocation.getArgumentAt(1, JoinConnectionsCallback.class);
                callback.onFailure(error);
                return null;
            }

        }).when(primaryConnection).join(eq(secondaryConnection), any(JoinConnectionsCallback.class));

        context.setPrimaryConnection(primaryConnection);
        context.setSecondaryConnection(secondaryConnection);

        // when
        JoinConnectionsAction action = new JoinConnectionsAction();
        action.execute(CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionState.EXECUTING, CreateConnectionEvent.VALIDATED_PARAMETERS, context, stateMachine);

        // then
        verify(primaryConnection).join(eq(secondaryConnection), any(JoinConnectionsCallback.class));
        verify(stateMachine).fire(CreateConnectionEvent.FAILURE, context);
        assertEquals(error, context.getError());
    }

}
