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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ScheduleTimeoutActionTest {
    
    @Test
    public void testScheduleTimeout() {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = spy(new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel));
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ListeningScheduledExecutorService scheduler = mock(ListeningScheduledExecutorService.class);
        final long timeout = 10000;
        
        final ListenableScheduledFuture<?> timerFuture = mock(ListenableScheduledFuture.class);

        when(fsm.getContext()).thenReturn(context);
        doReturn(timerFuture).when(scheduler).schedule(any(TimeoutConnectionTask.class), eq(timeout), eq(TimeUnit.MILLISECONDS));
        
        txContext.set(MgcpLocalConnectionParameter.TIMEOUT, timeout);
        txContext.set(MgcpLocalConnectionParameter.SCHEDULER, scheduler);

        // when
        final ScheduleTimeoutAction action = new ScheduleTimeoutAction();
        action.execute(MgcpLocalConnectionState.IDLE, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.OPEN, txContext, fsm);

        // then
        verify(scheduler).schedule(any(TimeoutConnectionTask.class), eq(timeout), eq(TimeUnit.MILLISECONDS));
        verify(context).setTimerFuture(timerFuture);
    }

    @Test
    public void testNoTimeoutDefined() {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = spy(new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel));
        final MgcpLocalConnectionTransitionContext txContext = new MgcpLocalConnectionTransitionContext();
        final MgcpLocalConnectionFsm fsm = mock(MgcpLocalConnectionFsm.class);
        final ListeningScheduledExecutorService scheduler = mock(ListeningScheduledExecutorService.class);
        final long timeout = 0;
        
        final ListenableScheduledFuture<?> timerFuture = mock(ListenableScheduledFuture.class);
        
        when(fsm.getContext()).thenReturn(context);
        doReturn(timerFuture).when(scheduler).schedule(any(TimeoutConnectionTask.class), eq(timeout), eq(TimeUnit.MILLISECONDS));
        
        txContext.set(MgcpLocalConnectionParameter.TIMEOUT, timeout);
        txContext.set(MgcpLocalConnectionParameter.SCHEDULER, scheduler);
        
        // when
        final ScheduleTimeoutAction action = new ScheduleTimeoutAction();
        action.execute(MgcpLocalConnectionState.IDLE, MgcpLocalConnectionState.OPEN, MgcpLocalConnectionEvent.OPEN, txContext, fsm);
        
        // then
        verify(scheduler, never()).schedule(any(TimeoutConnectionTask.class), eq(timeout), eq(TimeUnit.MILLISECONDS));
        verify(context, never()).setTimerFuture(timerFuture);
    }

}
