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
import org.restcomm.media.control.mgcp.pkg.MgcpActionType;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.NOTIFICATION_REQUEST;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.IDLE;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RemoveTimeoutSignalActionTest {

    @Test
    public void testRemoveTimeoutSignalWithPendingTimeoutSignalsAndNoBriefSignals() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[]{timeoutSignal1, timeoutSignal2};

        final BriefSignal[] briefSignals = new BriefSignal[0];

        final String eventPkg = "AU";
        final String eventSymbol = "oc";

        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent};

        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setRequestedEvents(requestedEvents);
        Collections.addAll(context.getTimeoutSignals(), timeoutSignals);
        Collections.addAll(context.getPendingBriefSignals(), briefSignals);
        context.setActiveBriefSignal(null);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);

        final RemoveTimeoutSignalAction action = new RemoveTimeoutSignalAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        verify(timeoutSignal2, never()).cancel(any(TimeoutSignalCancellationCallback.class));
        assertFalse(context.getTimeoutSignals().contains(timeoutSignal1));
        assertTrue(context.getTimeoutSignals().contains(timeoutSignal2));

        verify(stateMachine, never()).notify(any(), any(MgcpEvent.class));

        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testRemoveTimeoutSignalWithoutPendingTimeoutSignalsAndWithActiveBriefSignal() {
        // given
        final TimeoutSignal timeoutSignal = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[]{timeoutSignal};

        final BriefSignal briefSignal = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[0];

        final String eventPkg = "AU";
        final String eventSymbol = "oc";

        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent};

        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        context.setEndpointId(endpointId.toString());

        context.setRequestedEvents(requestedEvents);
        Collections.addAll(context.getTimeoutSignals(), timeoutSignals);
        Collections.addAll(context.getPendingBriefSignals(), briefSignals);
        context.setActiveBriefSignal(briefSignal);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);

        final RemoveTimeoutSignalAction action = new RemoveTimeoutSignalAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        assertTrue(context.getTimeoutSignals().isEmpty());
        assertEquals(briefSignal, context.getActiveBriefSignal());

        verify(stateMachine, never()).notify(any(), any(MgcpEvent.class));

        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testRemoveTimeoutSignalWithoutPendingSignals() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[]{timeoutSignal1};

        final BriefSignal[] briefSignals = new BriefSignal[0];

        final String eventPkg = "AU";
        final String eventSymbol = "oc";

        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent};

        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        context.setEndpointId(endpointId.toString());

        context.setRequestedEvents(requestedEvents);
        Collections.addAll(context.getTimeoutSignals(), timeoutSignals);
        Collections.addAll(context.getPendingBriefSignals(), briefSignals);
        context.setActiveBriefSignal(null);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);

        final RemoveTimeoutSignalAction action = new RemoveTimeoutSignalAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        assertTrue(context.getTimeoutSignals().isEmpty());

        verify(stateMachine, never()).notify(any(), any(MgcpEvent.class));

        verify(stateMachine).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

}
