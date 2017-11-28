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

import com.google.common.util.concurrent.FutureCallback;
import org.junit.Test;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 28/11/2017
 */
public class RaiseQuarantinedEventActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRaiseQuarantinedEvent() {
        // given
        final String requestId = "2A";
        final String signal = "pa";

        final SignalQuarantine quarantine = mock(SignalQuarantine.class);
        final MgcpEvent event = mock(MgcpEvent.class);

        when(quarantine.getRequestId()).thenReturn(requestId);

        final NotificationCenterContext context = new NotificationCenterContext();
        context.setQuarantine(quarantine);

        final NotificationCenterFsm fsm = mock(NotificationCenterFsm.class);
        when(fsm.getContext()).thenReturn(context);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);

        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);
        txContext.set(NotificationCenterTransitionParameter.REQUEST_IDENTIFIER, requestId);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, signal);

        final RaiseQuarantinedEventAction action = new RaiseQuarantinedEventAction();
        action.execute(NotificationCenterState.ACTIVE, NotificationCenterState.ACTIVE, NotificationCenterEvent.QUERY_QUARANTINED, txContext, fsm);

        // then
        verify(quarantine).getSignalResult(signal, callback);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQuarantinedRequestIdMismatch() {
        // given
        final String requestId = "2A";
        final String quarantinedRequestId = "2B";
        final String signal = "pa";

        final SignalQuarantine quarantine = mock(SignalQuarantine.class);
        final MgcpEvent event = mock(MgcpEvent.class);

        when(quarantine.getRequestId()).thenReturn(quarantinedRequestId);

        final NotificationCenterContext context = new NotificationCenterContext();
        context.setQuarantine(quarantine);

        final NotificationCenterFsm fsm = mock(NotificationCenterFsm.class);
        when(fsm.getContext()).thenReturn(context);

        // when
        final FutureCallback<MgcpEvent> callback = mock(FutureCallback.class);

        final NotificationCenterTransitionContext txContext = new NotificationCenterTransitionContext();
        txContext.set(NotificationCenterTransitionParameter.CALLBACK, callback);
        txContext.set(NotificationCenterTransitionParameter.REQUEST_IDENTIFIER, requestId);
        txContext.set(NotificationCenterTransitionParameter.SIGNAL, signal);

        final RaiseQuarantinedEventAction action = new RaiseQuarantinedEventAction();
        action.execute(NotificationCenterState.ACTIVE, NotificationCenterState.ACTIVE, NotificationCenterEvent.QUERY_QUARANTINED, txContext, fsm);

        // then
        verify(quarantine, never()).getSignalResult(signal, callback);
        verify(callback).onFailure(any(Throwable.class));
    }

}
