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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UpdateSecondaryConnectionModeActionTest {

    @Test
    public void testUpdateSecondaryConnectionMode() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpConnection connection = mock(MgcpConnection.class);
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;

        context.setSecondaryConnection(connection);
        context.setConnectionMode(mode);

        // when
        UpdateSecondaryConnectionModeAction action = new UpdateSecondaryConnectionModeAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> captor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(mode), captor.capture());

        // when
        final UpdateConnectionModeCallback callback = captor.getValue();
        callback.onSuccess(null);

        // then
        verify(stateMachine).fire(CreateConnectionEvent.CONNECTION_MODE_UPDATED, context);
        assertNull(context.getError());
    }

    @Test
    public void testUpdateSecondaryConnectionModeFailure() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final MgcpConnection connection = mock(MgcpConnection.class);
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;

        context.setSecondaryConnection(connection);
        context.setConnectionMode(mode);

        // when
        UpdateSecondaryConnectionModeAction action = new UpdateSecondaryConnectionModeAction();
        action.execute(CreateConnectionState.EXECUTING, CreateConnectionState.ROLLING_BACK, CreateConnectionEvent.FAILURE, context, stateMachine);

        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> captor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(mode), captor.capture());

        // when
        final UpdateConnectionModeCallback callback = captor.getValue();
        final Exception error = new Exception("test purposes");
        callback.onFailure(error);

        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertEquals(error, context.getError());
    }

}
