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

import org.junit.Test;
import org.restcomm.media.rtp.LocalDataChannel;

import com.google.common.util.concurrent.ListenableScheduledFuture;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CancelTimerActionTest {

    @Test
    public void testCancelTimer() {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ListenableScheduledFuture<?> timerFuture = mock(ListenableScheduledFuture.class);

        when(fsm.getContext()).thenReturn(context);

        context.setTimerFuture(timerFuture);
        when(timerFuture.isDone()).thenReturn(false);

        // when
        final CancelTimerAction action = new CancelTimerAction();
        action.execute(MgcpLocalConnectionState.HALF_OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.OPEN, txContext, fsm);

        // then
        verify(timerFuture).cancel(any(Boolean.class));
    }

    @Test
    public void testCancelTimerWhenFutureAlreadyDone() {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ListenableScheduledFuture<?> timerFuture = mock(ListenableScheduledFuture.class);

        when(fsm.getContext()).thenReturn(context);

        context.setTimerFuture(timerFuture);
        when(timerFuture.isDone()).thenReturn(true);

        // when
        final CancelTimerAction action = new CancelTimerAction();
        action.execute(MgcpLocalConnectionState.HALF_OPEN, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.OPEN, txContext, fsm);

        // then
        verify(timerFuture, never()).cancel(any(Boolean.class));
    }

}
