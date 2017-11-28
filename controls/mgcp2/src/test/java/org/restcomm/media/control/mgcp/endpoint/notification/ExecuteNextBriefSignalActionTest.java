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

package org.restcomm.media.control.mgcp.endpoint.notification;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;

import java.util.Collections;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.signal.BriefSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ExecuteNextBriefSignalActionTest {

    @Test
    public void testExecuteNextBriefSignal() {
        // given
        final BriefSignal activeBriefSignal = mock(BriefSignal.class);
        final BriefSignal pendingBriefSignal1 = mock(BriefSignal.class);
        final BriefSignal[] pendingBriefSignals = new BriefSignal[] { pendingBriefSignal1 };

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        context.setEndpointId(endpointId.toString());
        
        context.setActiveBriefSignal(activeBriefSignal);
        Collections.addAll(context.getPendingBriefSignals(), pendingBriefSignals);

        // when
        final ExecuteNextBriefSignalAction action = new ExecuteNextBriefSignalAction();
        
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, activeBriefSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, null);
        
        action.execute(ACTIVE, ACTIVE, NotificationCenterEvent.SIGNAL_EXECUTED, txContext, stateMachine);

        // then
        verify(pendingBriefSignal1).execute(any(BriefSignalExecutionCallback.class));
        verify(context).setActiveBriefSignal(pendingBriefSignal1);
        assertFalse(context.getPendingBriefSignals().contains(pendingBriefSignal1));

        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testNoPendingBriefSignals() {
        // given
        final BriefSignal activeBriefSignal = mock(BriefSignal.class);
        final BriefSignal[] pendingBriefSignals = new BriefSignal[0];
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        context.setEndpointId(endpointId.toString());
        
        context.setActiveBriefSignal(activeBriefSignal);
        Collections.addAll(context.getPendingBriefSignals(), pendingBriefSignals);
        
        // when
        final ExecuteNextBriefSignalAction action = new ExecuteNextBriefSignalAction();
        
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, activeBriefSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, null);
        
        action.execute(ACTIVE, ACTIVE, NotificationCenterEvent.SIGNAL_EXECUTED, txContext, stateMachine);
        
        // then
        verify(context).setActiveBriefSignal(null);
        assertTrue(context.getPendingBriefSignals().isEmpty());
        verify(stateMachine).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

}
