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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.NOTIFICATION_REQUEST;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.IDLE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterTransitionParameter.*;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.collections.Sets;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationActionTest {

    @Test
    public void testRequestNotification() {
        // given
        final String requestId = "12345";
        final NotifiedEntity notifiedEntity = new NotifiedEntity("restcomm", "127.0.0.1", 2727);
        final MgcpRequestedEvent requestedEvent1 = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent requestedEvent2 = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent1, requestedEvent2 };
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        
        final List<MgcpSignal<?>> requestedSignals = Arrays.asList(timeoutSignal1, briefSignal1, briefSignal2, timeoutSignal2, briefSignal3, timeoutSignal3);
        final Set<TimeoutSignal> requestedTimeoutSignals = Sets.newSet(timeoutSignal1, timeoutSignal2, timeoutSignal3);
        final List<BriefSignal> requestedBriefSignals = Arrays.asList(briefSignal1, briefSignal2, briefSignal3);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(REQUEST_IDENTIFIER, requestId);
        txContext.set(NOTIFIED_ENTITY, notifiedEntity);
        txContext.set(REQUESTED_EVENTS, requestedEvents);
        txContext.set(REQUESTED_SIGNALS, requestedSignals);
        txContext.set(REQUESTED_TIMEOUT_SIGNALS, requestedTimeoutSignals);
        txContext.set(REQUESTED_BRIEF_SIGNALS, requestedBriefSignals);

        // when
        final RequestNotificationAction action = new RequestNotificationAction();
        action.execute(IDLE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        ArgumentCaptor<MgcpRequestedEvent[]> eventCaptor = ArgumentCaptor.forClass(MgcpRequestedEvent[].class);

        verify(context).setRequestId(requestId);
        verify(context).setNotifiedEntity(notifiedEntity);
        verify(context).setRequestedEvents(eventCaptor.capture());
        
        final MgcpRequestedEvent[] events = eventCaptor.getValue();
        assertEquals(2, events.length);
        assertEquals(requestedEvent1, events[0]);
        assertEquals(requestedEvent2, events[1]);

        final Set<TimeoutSignal> timeoutSignals = context.getTimeoutSignals();
        assertEquals(3, timeoutSignals.size());
        assertTrue(timeoutSignals.contains(timeoutSignal1));
        assertTrue(timeoutSignals.contains(timeoutSignal2));
        assertTrue(timeoutSignals.contains(timeoutSignal3));

        final Queue<BriefSignal> briefSignals = context.getPendingBriefSignals();
        assertEquals(3, briefSignals.size());
        assertEquals(briefSignal1, briefSignals.poll());
        assertEquals(briefSignal2, briefSignals.poll());
        assertEquals(briefSignal3, briefSignals.poll());
    }

}
