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
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.NOTIFICATION_REQUEST;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.IDLE;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpActionType;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EvaluateSignalResultActionTest {

    @Test
    public void testEvaluateRequestedSignalResultWhileActiveBriefSignal() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal1, timeoutSignal2, timeoutSignal3 };
        
        final BriefSignal activeBriefSignal = mock(BriefSignal.class);
        final BriefSignal pendingBriefSignal1 = mock(BriefSignal.class);
        final BriefSignal pendingBriefSignal2 = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[] { pendingBriefSignal1, pendingBriefSignal2 };
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent};
        
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setRequestedEvents(requestedEvents);
        context.setTimeoutSignals(timeoutSignals);
        context.setActiveBriefSignal(activeBriefSignal);
        context.setPendingBriefSignals(briefSignals);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);
        
        final EvaluateSignalResultAction action = new EvaluateSignalResultAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        verify(timeoutSignal1, never()).execute(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));
        assertTrue(context.getTimeoutSignals().isEmpty());
        
        verify(pendingBriefSignal1, never()).execute(any(BriefSignalExecutionCallback.class));
        assertTrue(context.getPendingBriefSignals().isEmpty());
        assertEquals(context.getActiveBriefSignal(), activeBriefSignal);
        
        verify(endpoint).onEvent(any(), eq(event));
        
        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testEvaluateRequestedSignalResultWithoutActiveBriefSignal() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal1, timeoutSignal2, timeoutSignal3 };
        
        final BriefSignal[] briefSignals = new BriefSignal[0];
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent};
        
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        
        context.setRequestedEvents(requestedEvents);
        context.setTimeoutSignals(timeoutSignals);
        context.setActiveBriefSignal(null);
        context.setPendingBriefSignals(briefSignals);
        
        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);
        
        final EvaluateSignalResultAction action = new EvaluateSignalResultAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);
        
        // then
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));
        assertTrue(context.getTimeoutSignals().isEmpty());
        
        verify(endpoint).onEvent(any(), eq(event));
        
        verify(stateMachine).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }
    
    @Test
    public void testEvaluateUnrequestedSignalResultWithActiveBriefSignal() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal1, timeoutSignal2, timeoutSignal3 };
        
        final BriefSignal activeBriefSignal = mock(BriefSignal.class);
        final BriefSignal pendingBriefSignal1 = mock(BriefSignal.class);
        final BriefSignal pendingBriefSignal2 = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[] { pendingBriefSignal1, pendingBriefSignal2 };
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[0];
        
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setRequestedEvents(requestedEvents);
        context.setTimeoutSignals(timeoutSignals);
        context.setActiveBriefSignal(activeBriefSignal);
        context.setPendingBriefSignals(briefSignals);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);
        
        final EvaluateSignalResultAction action = new EvaluateSignalResultAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        verify(timeoutSignal2, never()).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3, never()).cancel(any(TimeoutSignalCancellationCallback.class));
        assertFalse(context.getTimeoutSignals().contains(timeoutSignal1));
        
        verify(pendingBriefSignal1, never()).execute(any(BriefSignalExecutionCallback.class));
        assertFalse(context.getPendingBriefSignals().isEmpty());
        assertEquals(context.getActiveBriefSignal(), activeBriefSignal);
        
        verify(endpoint, never()).onEvent(any(), eq(event));
        
        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testEvaluateUnrequestedSignalResultWithoutPendingSignals() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal1 };
        
        final BriefSignal[] briefSignals = new BriefSignal[0];
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[0];
        
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        
        context.setRequestedEvents(requestedEvents);
        context.setTimeoutSignals(timeoutSignals);
        context.setActiveBriefSignal(null);
        context.setPendingBriefSignals(briefSignals);
        
        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, timeoutSignal1);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);
        
        final EvaluateSignalResultAction action = new EvaluateSignalResultAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);
        
        // then
        assertTrue(context.getTimeoutSignals().isEmpty());
        verify(endpoint, never()).onEvent(any(), eq(event));
        verify(stateMachine).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }

    @Test
    public void testEvaluateSignalResultFromUnrecognizedSignal() {
        // given
        final TimeoutSignal unrecognizedTimeoutSignal = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal1, timeoutSignal2 };
        
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[] { briefSignal2 };
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent};
        
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn(eventPkg);
        when(event.getSymbol()).thenReturn(eventSymbol);
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        
        context.setRequestedEvents(requestedEvents);
        context.setTimeoutSignals(timeoutSignals);
        context.setActiveBriefSignal(briefSignal1);
        context.setPendingBriefSignals(briefSignals);
        
        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, unrecognizedTimeoutSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, event);
        
        final EvaluateSignalResultAction action = new EvaluateSignalResultAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);
        
        // then
        verify(timeoutSignal2, never()).cancel(any(TimeoutSignalCancellationCallback.class));
        assertFalse(context.getTimeoutSignals().isEmpty());
        assertFalse(context.getPendingBriefSignals().isEmpty());
        
        verify(endpoint, never()).onEvent(any(), eq(event));
        
        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }
}
