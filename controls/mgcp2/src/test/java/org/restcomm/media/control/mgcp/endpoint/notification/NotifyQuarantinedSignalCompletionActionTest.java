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

import org.junit.Test;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import static org.mockito.Mockito.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.STOP;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.IDLE;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class NotifyQuarantinedSignalCompletionActionTest {

    @Test
    public void testNotifyQuarantinedSignalCompletion() {
        // given
        final MgcpEvent event = mock(MgcpEvent.class);
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);

        final SignalQuarantine quarantine = mock(SignalQuarantine.class);
        when(quarantine.contains(timeoutSignal1)).thenReturn(true);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setEndpointId(endpointId.toString());
        context.setQuarantine(quarantine);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);

        final NotifyQuarantinedSignalCompletionAction action = NotifyQuarantinedSignalCompletionAction.INSTANCE;
        action.execute(ACTIVE, IDLE, STOP, txContext, stateMachine);

        // then
        verify(quarantine).onSignalCompleted(timeoutSignal1, event);
    }

    @Test
    public void testNotifyUnknownSignalCompletion() {
        // given
        final MgcpEvent event = mock(MgcpEvent.class);
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);

        final SignalQuarantine quarantine = mock(SignalQuarantine.class);
        when(quarantine.contains(timeoutSignal1)).thenReturn(false);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setEndpointId(endpointId.toString());
        context.setQuarantine(quarantine);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);

        final NotifyQuarantinedSignalCompletionAction action = NotifyQuarantinedSignalCompletionAction.INSTANCE;
        action.execute(ACTIVE, IDLE, STOP, txContext, stateMachine);

        // then
        verify(quarantine, never()).onSignalCompleted(timeoutSignal1, event);
    }

}
