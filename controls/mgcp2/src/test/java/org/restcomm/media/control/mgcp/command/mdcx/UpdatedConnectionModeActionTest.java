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

package org.restcomm.media.control.mgcp.command.mdcx;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UpdatedConnectionModeActionTest {

    @Test
    public void testUpdateConnectionMode() {
        // given
        final int transactionId = 12345;
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;
        final MgcpConnection connection = mock(MgcpConnection.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm fsm = mock(ModifyConnectionFsm.class);

        context.setConnection(connection);
        context.setMode(mode);

        // when
        UpdateConnectionModeAction action = new UpdateConnectionModeAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.MODIFYING_CONNECTION, ModifyConnectionEvent.EXECUTE, context, fsm);

        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> captor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(mode), captor.capture());

        // when
        final UpdateConnectionModeCallback callback = captor.getValue();
        callback.onSuccess(null);

        // then
        verify(fsm).fire(ModifyConnectionEvent.UPDATED_CONNECTION_MODE, context);
    }

    @Test
    public void testUpdateConnectionModeFailure() {
        // given
        final int transactionId = 12345;
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;
        final MgcpConnection connection = mock(MgcpConnection.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm fsm = mock(ModifyConnectionFsm.class);
        
        context.setConnection(connection);
        context.setMode(mode);
        
        // when
        UpdateConnectionModeAction action = new UpdateConnectionModeAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.MODIFYING_CONNECTION, ModifyConnectionEvent.EXECUTE, context, fsm);
        
        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> captor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(mode), captor.capture());
        
        // when
        final UpdateConnectionModeCallback callback = captor.getValue();
        final Exception exception = new Exception("test purposes");
        callback.onFailure(exception);
        
        // then
        verify(fsm).fireImmediate(ModifyConnectionEvent.FAILURE, context);
    }

}
