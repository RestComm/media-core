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

import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Test;
import org.restcomm.media.rtp.LocalDataChannel;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class JoinActionTest {

    @Test
    public void testJoin() throws IOException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);

        final int identifier2 = 1;
        final int callIdentifier2 = 1;
        final LocalDataChannel dataChannel2 = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context2 = new MgcpLocalConnectionContext(identifier2, callIdentifier2, dataChannel2);
        final MgcpLocalConnectionImpl joinee = mock(MgcpLocalConnectionImpl.class);
        
        when(fsm.getContext()).thenReturn(context);
        when(joinee.getContext()).thenReturn(context2);

        // when
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        txContext.set(MgcpLocalConnectionParameter.JOINEE, joinee);

        final JoinAction action = new JoinAction();
        action.execute(MgcpLocalConnectionState.OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.JOIN, txContext, fsm);

        // then
        verify(dataChannel).join(dataChannel2);
    }

    @Test
    public void testJoinFailure() throws IOException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        
        final int identifier2 = 1;
        final int callIdentifier2 = 1;
        final LocalDataChannel dataChannel2 = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context2 = new MgcpLocalConnectionContext(identifier2, callIdentifier2, dataChannel2);
        final MgcpLocalConnectionImpl joinee = mock(MgcpLocalConnectionImpl.class);
        
        final FutureCallback<?> callback = mock(FutureCallback.class);
        
        when(fsm.getContext()).thenReturn(context);
        when(joinee.getContext()).thenReturn(context2);
        
        doThrow(new IOException()).when(dataChannel).join(dataChannel2);
        
        // when
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        txContext.set(MgcpLocalConnectionParameter.JOINEE, joinee);
        txContext.set(MgcpLocalConnectionParameter.CALLBACK, callback);
        
        final JoinAction action = new JoinAction();
        action.execute(MgcpLocalConnectionState.OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.JOIN, txContext, fsm);
        
        // then
        verify(dataChannel).join(dataChannel2);
        verify(fsm).fire(MgcpLocalConnectionEvent.FAILURE, txContext);
    }
    
    
}
