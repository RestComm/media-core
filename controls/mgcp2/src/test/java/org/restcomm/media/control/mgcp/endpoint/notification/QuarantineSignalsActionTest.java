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
import org.mockito.internal.util.collections.Sets;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.STOP;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.ACTIVE;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.IDLE;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class QuarantineSignalsActionTest {

    @Test
    public void testQuarantineSignals() {
        // given
        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final Set<TimeoutSignal> timeoutSignals = Sets.newSet(timeoutSignal1, timeoutSignal2, timeoutSignal3);
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext());
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);

        context.setEndpointId(endpointId.toString());

        final SignalQuarantine quarantine = mock(SignalQuarantine.class);

        context.setTimeoutSignals(timeoutSignals);
        context.setQuarantine(quarantine);

        // when
        final QuarantineSignalsAction action = new QuarantineSignalsAction();
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        action.execute(ACTIVE, IDLE, STOP, txContext, stateMachine);

        // then
        verify(quarantine).close();

        assertNotNull(context.getQuarantine());
        assertTrue(context.getQuarantine().contains(timeoutSignal1));
        assertTrue(context.getQuarantine().contains(timeoutSignal2));
        assertTrue(context.getQuarantine().contains(timeoutSignal3));
    }

}
