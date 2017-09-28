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
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterEvent.*;
import static org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenterState.*;

import java.util.Collections;

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
public class RemoveActiveBriefSignalActionTest {

    @Test
    public void testRemoveBriefSignalWithPendingTimeoutSignals() {
        // given
        final TimeoutSignal timeoutSignal = mock(TimeoutSignal.class);
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[] { timeoutSignal };
        
        final BriefSignal briefSignal = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[0];
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent};
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        
        context.setRequestedEvents(requestedEvents);
        Collections.addAll(context.getTimeoutSignals(), timeoutSignals);
        Collections.addAll(context.getPendingBriefSignals(), briefSignals);
        context.setActiveBriefSignal(briefSignal);
        
        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, briefSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, null);
        
        final RemoveActiveBriefSignalAction action = new RemoveActiveBriefSignalAction();
        action.execute(DEACTIVATING, DEACTIVATING, SIGNAL_EXECUTED, txContext, stateMachine);
        
        // then
        assertNull(context.getActiveBriefSignal());
        verify(endpoint, never()).onEvent(any(), any(MgcpEvent.class));
        verify(stateMachine, never()).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }
    
    @Test
    public void testRemoveBriefSignalWithoutPendingTimeoutSignals() {
        // given
        final TimeoutSignal[] timeoutSignals = new TimeoutSignal[0];
        
        final BriefSignal briefSignal = mock(BriefSignal.class);
        final BriefSignal[] briefSignals = new BriefSignal[0];
        
        final String eventPkg = "AU";
        final String eventSymbol = "oc";
        final MgcpRequestedEvent requestedEvent = new MgcpRequestedEvent("1A", eventPkg, eventSymbol, MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent};
        
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = spy(new NotificationCenterContext(endpoint));
        final NotificationCenterFsm stateMachine = mock(NotificationCenterFsm.class);
        when(stateMachine.getContext()).thenReturn(context);
        
        context.setRequestedEvents(requestedEvents);
        Collections.addAll(context.getTimeoutSignals(), timeoutSignals);
        Collections.addAll(context.getPendingBriefSignals(), briefSignals);
        context.setActiveBriefSignal(briefSignal);
        
        // when
        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, briefSignal);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL_RESULT, null);
        
        final RemoveActiveBriefSignalAction action = new RemoveActiveBriefSignalAction();
        action.execute(DEACTIVATING, DEACTIVATING, SIGNAL_EXECUTED, txContext, stateMachine);
        
        // then
        assertNull(context.getActiveBriefSignal());
        verify(endpoint, never()).onEvent(any(), any(MgcpEvent.class));
        verify(stateMachine).fire(eq(NotificationCenterEvent.ALL_SIGNALS_COMPLETED), any(NotificationCenterTransitionContext.class));
    }


}
