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
        
package org.restcomm.media.control.mgcp.connection.local;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ModeNotSupportedException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UpdateModeActionTest {
    
    @Test
    public void testUpdateMode() throws ModeNotSupportedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = spy(new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel));
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        
        when(fsm.getContext()).thenReturn(context);
        
        txContext.set(MgcpLocalConnectionParameter.MODE, mode);

        // when
        final UpdateModeAction action = new UpdateModeAction();
        action.execute(MgcpLocalConnectionState.OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.UPDATE_MODE, txContext, fsm);

        // then
        verify(dataChannel).updateMode(mode);
        verify(context).setMode(mode);
    }

    @Test
    public void testDontUpdateSameMode() throws ModeNotSupportedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = spy(new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel));
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ConnectionMode mode = ConnectionMode.INACTIVE;
        
        when(fsm.getContext()).thenReturn(context);
        
        // when
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        txContext.set(MgcpLocalConnectionParameter.MODE, mode);

        final UpdateModeAction action = new UpdateModeAction();
        action.execute(MgcpLocalConnectionState.OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.UPDATE_MODE, txContext, fsm);
        
        // then
        verify(dataChannel, never()).updateMode(mode);
        verify(context, never()).setMode(mode);
    }
    
    @Test
    public void testUpdateModeFailure() throws ModeNotSupportedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = spy(new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel));
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        
        when(fsm.getContext()).thenReturn(context);
        doThrow(new ModeNotSupportedException(mode)).when(dataChannel).updateMode(mode);
        
        txContext.set(MgcpLocalConnectionParameter.MODE, mode);

        // when
        final UpdateModeAction action = new UpdateModeAction();
        action.execute(MgcpLocalConnectionState.OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.UPDATE_MODE, txContext, fsm);

        // then
        verify(dataChannel).updateMode(mode);
        verify(context, never()).setMode(mode);
        verify(fsm).fireImmediate(MgcpLocalConnectionEvent.FAILURE, txContext);
    }

}
