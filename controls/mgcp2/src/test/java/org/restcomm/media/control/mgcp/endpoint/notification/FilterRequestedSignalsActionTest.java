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
import static org.mockito.Mockito.when;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.NOTIFICATION_REQUEST;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FilterRequestedSignalsActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testFilterSignals() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal4 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal5 = mock(TimeoutSignal.class);

        final List<TimeoutSignal> activeTimeoutSignals = Arrays.asList(timeoutSignal1, timeoutSignal2, timeoutSignal3);

        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        final BriefSignal briefSignal4 = mock(BriefSignal.class);
        final BriefSignal briefSignal5 = mock(BriefSignal.class);
        final BriefSignal briefSignal6 = mock(BriefSignal.class);

        final List<BriefSignal> pendingBriefSignals = Arrays.asList(briefSignal2, briefSignal3);

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setEndpointId(endpointId.toString());
        context.setActiveBriefSignal(briefSignal1);
        context.setPendingBriefSignals(pendingBriefSignals);
        context.setTimeoutSignals(activeTimeoutSignals);

        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.REQUESTED_SIGNALS, Arrays.asList(timeoutSignal1, timeoutSignal4, briefSignal4, briefSignal6, timeoutSignal5, briefSignal5));

        final FilterRequestedSignalsAction action = new FilterRequestedSignalsAction();
        action.execute(ACTIVE, ACTIVE, NOTIFICATION_REQUEST, txContext, stateMachine);

        // then
        final Set<TimeoutSignal> requestedTimeoutSignals = txContext.get(NotificationCenterTransitionParameter.REQUESTED_TIMEOUT_SIGNALS, Set.class);
        assertEquals(3, requestedTimeoutSignals.size());
        assertTrue(requestedTimeoutSignals.contains(timeoutSignal1));
        assertTrue(requestedTimeoutSignals.contains(timeoutSignal4));
        assertTrue(requestedTimeoutSignals.contains(timeoutSignal5));

        final Set<TimeoutSignal> unrequestedTimeoutSignals = txContext.get(NotificationCenterTransitionParameter.UNREQUESTED_TIMEOUT_SIGNALS, Set.class);
        assertEquals(2, unrequestedTimeoutSignals.size());
        assertTrue(unrequestedTimeoutSignals.contains(timeoutSignal2));
        assertTrue(unrequestedTimeoutSignals.contains(timeoutSignal3));

        final Set<TimeoutSignal> pendingTimeoutSignals = txContext.get(NotificationCenterTransitionParameter.PENDING_TIMEOUT_SIGNALS, Set.class);
        assertEquals(2, pendingTimeoutSignals.size());
        assertTrue(pendingTimeoutSignals.contains(timeoutSignal4));
        assertTrue(pendingTimeoutSignals.contains(timeoutSignal5));

        final List<BriefSignal> requestedBriefSignals = txContext.get(NotificationCenterTransitionParameter.REQUESTED_BRIEF_SIGNALS, List.class);
        assertEquals(3, requestedBriefSignals.size());
        assertEquals(briefSignal4, requestedBriefSignals.get(0));
        assertEquals(briefSignal6, requestedBriefSignals.get(1));
        assertEquals(briefSignal5, requestedBriefSignals.get(2));
    }

}
