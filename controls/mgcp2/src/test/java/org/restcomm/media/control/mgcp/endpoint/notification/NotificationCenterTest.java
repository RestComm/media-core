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
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.command.rqnt.NotificationRequest;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpActionType;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class NotificationCenterTest {

    private NotificationCenterFsm fsm;

    @After
    public void after() {
        if (fsm != null && fsm.isStarted()) {
            fsm.terminate();
            fsm = null;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRqntWithTimeoutAndBriefSignals() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        when(briefSignal1.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        when(briefSignal2.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        when(briefSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, briefSignal1, briefSignal2, timeoutSignal3, briefSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final NotificationRequest rqnt = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt, callback);

        // then
        final ArgumentCaptor<TimeoutSignalExecutionCallback> timeoutCallbackCaptor = ArgumentCaptor.forClass(TimeoutSignalExecutionCallback.class);
        final ArgumentCaptor<BriefSignalExecutionCallback> briefCallbackCaptor = ArgumentCaptor.forClass(BriefSignalExecutionCallback.class);

        verify(timeoutSignal1).execute(timeoutCallbackCaptor.capture());
        verify(timeoutSignal2).execute(any(TimeoutSignalExecutionCallback.class));
        verify(timeoutSignal3).execute(any(TimeoutSignalExecutionCallback.class));
        verify(briefSignal1).execute(briefCallbackCaptor.capture());
        verify(briefSignal2, never()).execute(any(BriefSignalExecutionCallback.class));
        verify(briefSignal3, never()).execute(any(BriefSignalExecutionCallback.class));
        verify(callback).onSuccess(null);

        // when - BR signal completes
        briefCallbackCaptor.getValue().onSuccess(null);

        // then
        verify(briefSignal2).execute(any(BriefSignalExecutionCallback.class));
        verify(briefSignal3, never()).execute(any(BriefSignalExecutionCallback.class));
        assertEquals(1, context.getPendingBriefSignals().size());

        // when - TO signal times out
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn("AU");
        when(event.getSymbol()).thenReturn("oc");

        timeoutCallbackCaptor.getValue().onSuccess(event);

        // then
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(endpoint).onEvent(any(), eq(event));

        assertTrue(context.getPendingBriefSignals().isEmpty());
        assertTrue(context.getTimeoutSignals().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCancelAllOngoingSignals() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        when(briefSignal1.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        when(briefSignal2.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        when(briefSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, briefSignal1, briefSignal2, timeoutSignal3, briefSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // then
        verify(callback1, timeout(50)).onSuccess(null);

        // when - override signals
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, new MgcpSignal<?>[0]);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        verify(callback2, timeout(50)).onSuccess(null);
        verify(timeoutSignal1).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));

        assertTrue(context.getPendingBriefSignals().isEmpty());
        assertTrue(context.getTimeoutSignals().isEmpty());
        assertNotNull(context.getActiveBriefSignal());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCancelTimeoutSignalsAndRequestBriefSignals() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // then
        verify(callback1, timeout(50)).onSuccess(null);

        // when - override signals
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, briefSignal1, briefSignal2);
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        verify(callback2, timeout(50)).onSuccess(null);
        verify(timeoutSignal1).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));

        assertTrue(context.getTimeoutSignals().isEmpty());
        assertEquals(1, context.getPendingBriefSignals().size());
        assertTrue(context.getPendingBriefSignals().contains(briefSignal2));
        assertEquals(briefSignal1, context.getActiveBriefSignal());

        final SignalQuarantine quarantine = context.getQuarantine();
        assertNotNull(quarantine);
        assertTrue(quarantine.contains(timeoutSignal1));
        assertTrue(quarantine.contains(timeoutSignal2));
        assertTrue(quarantine.contains(timeoutSignal3));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQuarantinedEventsWhileActive() throws InterruptedException {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // when - override signals
        final String requestId2 = "666";
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId2, notifiedEntity, requestedEvents, briefSignal1, briefSignal2);
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        final ArgumentCaptor<TimeoutSignalCancellationCallback> timeout1CancelCallback = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        final ArgumentCaptor<TimeoutSignalCancellationCallback> timeout2CancelCallback = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        final ArgumentCaptor<TimeoutSignalExecutionCallback> timeout3ExecutionCallback = ArgumentCaptor.forClass(TimeoutSignalExecutionCallback.class);

        verify(timeoutSignal1, timeout(50)).cancel(timeout1CancelCallback.capture());
        verify(timeoutSignal2, timeout(50)).cancel(timeout2CancelCallback.capture());
        verify(timeoutSignal3, timeout(50)).execute(timeout3ExecutionCallback.capture());

        final SignalQuarantine quarantine = context.getQuarantine();
        assertNotNull(quarantine);
        assertTrue(quarantine.contains(timeoutSignal1));
        assertTrue(quarantine.contains(timeoutSignal2));
        assertTrue(quarantine.contains(timeoutSignal3));

        // when - quarantined signals raise cancellation/execution events
        final MgcpEvent event1 = mock(MgcpEvent.class);
        final MgcpEvent event2 = mock(MgcpEvent.class);
        final MgcpEvent event3 = mock(MgcpEvent.class);

        timeout1CancelCallback.getValue().onSuccess(event1);
        timeout2CancelCallback.getValue().onSuccess(event2);
        timeout3ExecutionCallback.getValue().onSuccess(event3);

        // then
        final FutureCallback<MgcpEvent> quarantineResultCallback1 = mock(FutureCallback.class);
        final FutureCallback<MgcpEvent> quarantineResultCallback2 = mock(FutureCallback.class);
        final FutureCallback<MgcpEvent> quarantineResultCallback3 = mock(FutureCallback.class);

        quarantine.getSignalResult(timeoutSignal1, quarantineResultCallback1);
        quarantine.getSignalResult(timeoutSignal2, quarantineResultCallback2);
        quarantine.getSignalResult(timeoutSignal3, quarantineResultCallback3);

        verify(quarantineResultCallback1).onSuccess(event1);
        verify(quarantineResultCallback2).onSuccess(event2);
        verify(quarantineResultCallback3).onSuccess(event3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQuarantinedEventsWhileIdle() throws InterruptedException {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // when - override signals
        final String requestId2 = "666";
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId2, notifiedEntity, requestedEvents, new MgcpSignal<?>[0]);
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        final ArgumentCaptor<TimeoutSignalCancellationCallback> timeout1CancelCallback = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        final ArgumentCaptor<TimeoutSignalCancellationCallback> timeout2CancelCallback = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        final ArgumentCaptor<TimeoutSignalExecutionCallback> timeout3ExecutionCallback = ArgumentCaptor.forClass(TimeoutSignalExecutionCallback.class);

        verify(timeoutSignal1, timeout(50)).cancel(timeout1CancelCallback.capture());
        verify(timeoutSignal2, timeout(50)).cancel(timeout2CancelCallback.capture());
        verify(timeoutSignal3, timeout(50)).execute(timeout3ExecutionCallback.capture());

        final SignalQuarantine quarantine = context.getQuarantine();
        assertNotNull(quarantine);
        assertTrue(quarantine.contains(timeoutSignal1));
        assertTrue(quarantine.contains(timeoutSignal2));
        assertTrue(quarantine.contains(timeoutSignal3));

        // when - quarantined signals raise cancellation/execution events
        final MgcpEvent event1 = mock(MgcpEvent.class);
        final MgcpEvent event2 = mock(MgcpEvent.class);
        final MgcpEvent event3 = mock(MgcpEvent.class);

        timeout1CancelCallback.getValue().onSuccess(event1);
        timeout2CancelCallback.getValue().onSuccess(event2);
        timeout3ExecutionCallback.getValue().onSuccess(event3);

        // then
        final FutureCallback<MgcpEvent> quarantineResultCallback1 = mock(FutureCallback.class);
        final FutureCallback<MgcpEvent> quarantineResultCallback2 = mock(FutureCallback.class);
        final FutureCallback<MgcpEvent> quarantineResultCallback3 = mock(FutureCallback.class);

        quarantine.getSignalResult(timeoutSignal1, quarantineResultCallback1);
        quarantine.getSignalResult(timeoutSignal2, quarantineResultCallback2);
        quarantine.getSignalResult(timeoutSignal3, quarantineResultCallback3);

        verify(quarantineResultCallback1).onSuccess(event1);
        verify(quarantineResultCallback2).onSuccess(event2);
        verify(quarantineResultCallback3).onSuccess(event3);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCancelSignalsButMaintainOneTimeout() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // then
        verify(callback1, timeout(50)).onSuccess(null);

        // when - override signals
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, timeoutSignal1);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        verify(callback2, timeout(50)).onSuccess(null);
        verify(timeoutSignal1, never()).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));

        assertTrue(context.getTimeoutSignals().contains(timeoutSignal1));
        assertEquals(1, context.getTimeoutSignals().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeactivationWithOngoingSignals() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        when(briefSignal1.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        when(briefSignal2.getRequestId()).thenReturn(requestId);

        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        when(briefSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, briefSignal1, briefSignal2, timeoutSignal3, briefSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt, wait for callback and then shutdown
        final FutureCallback<Void> rqntCallback = mock(FutureCallback.class);
        final NotificationRequest rqnt = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt, rqntCallback);
        verify(rqntCallback, timeout(50)).onSuccess(null);

        final FutureCallback<Void> shutdownCallback = mock(FutureCallback.class);
        notificationCenter.shutdown(shutdownCallback);

        // then
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback1 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback2 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback3 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);

        verify(timeoutSignal1).cancel(timeoutCancelCallback1.capture());
        verify(timeoutSignal2).cancel(timeoutCancelCallback2.capture());
        verify(timeoutSignal3).cancel(timeoutCancelCallback3.capture());
        assertEquals(3, context.getTimeoutSignals().size());
        assertTrue(context.getPendingBriefSignals().isEmpty());
        assertNotNull(context.getActiveBriefSignal());

        // when - TO signals canceled
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn("AU");
        when(event.getSymbol()).thenReturn("oc");

        timeoutCancelCallback1.getValue().onSuccess(event);
        timeoutCancelCallback2.getValue().onSuccess(event);
        timeoutCancelCallback3.getValue().onSuccess(event);

        // then
        assertTrue(context.getTimeoutSignals().isEmpty());
        verify(endpoint, never()).onEvent(any(), eq(event));

        // when - BR signal completes execution
        ArgumentCaptor<BriefSignalExecutionCallback> briefExecuteCallback1 = ArgumentCaptor.forClass(BriefSignalExecutionCallback.class);
        verify(briefSignal1).execute(briefExecuteCallback1.capture());

        briefExecuteCallback1.getValue().onSuccess(null);

        // then
        assertNull(context.getActiveBriefSignal());
        verify(shutdownCallback).onSuccess(null);
        assertEquals(NotificationCenterState.DEACTIVATED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeactivationWhileSignalTerminatesExecution() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt, wait for callback and then shutdown
        final FutureCallback<Void> rqntCallback = mock(FutureCallback.class);
        final NotificationRequest rqnt = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt, rqntCallback);
        verify(rqntCallback, timeout(50)).onSuccess(null);

        final FutureCallback<Void> shutdownCallback = mock(FutureCallback.class);
        notificationCenter.shutdown(shutdownCallback);

        // then
        ArgumentCaptor<TimeoutSignalExecutionCallback> timeoutExecutionCallback = ArgumentCaptor.forClass(TimeoutSignalExecutionCallback.class);
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback1 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback2 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);
        ArgumentCaptor<TimeoutSignalCancellationCallback> timeoutCancelCallback3 = ArgumentCaptor.forClass(TimeoutSignalCancellationCallback.class);

        verify(timeoutSignal1).execute(timeoutExecutionCallback.capture());

        verify(timeoutSignal1).cancel(timeoutCancelCallback1.capture());
        verify(timeoutSignal2).cancel(timeoutCancelCallback2.capture());
        verify(timeoutSignal3).cancel(timeoutCancelCallback3.capture());
        assertEquals(3, context.getTimeoutSignals().size());

        // when - TO signals terminated
        final MgcpEvent event = mock(MgcpEvent.class);
        when(event.getPackage()).thenReturn("AU");
        when(event.getSymbol()).thenReturn("oc");

        timeoutExecutionCallback.getValue().onSuccess(event);
        timeoutCancelCallback1.getValue().onFailure(new Exception("testing purposes"));
        timeoutCancelCallback2.getValue().onSuccess(event);
        timeoutCancelCallback3.getValue().onSuccess(event);

        // then
        assertTrue(context.getTimeoutSignals().isEmpty());
        verify(endpoint, never()).onEvent(any(), eq(event));
        verify(shutdownCallback).onSuccess(null);
        assertEquals(NotificationCenterState.DEACTIVATED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRequestNotificationWhileDeactivating() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        when(timeoutSignal2.getRequestId()).thenReturn(requestId);

        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        when(timeoutSignal3.getRequestId()).thenReturn(requestId);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1, timeoutSignal2, timeoutSignal3};


        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt with signals
        final FutureCallback<Void> rqntCallback = mock(FutureCallback.class);
        final NotificationRequest rqnt = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt, rqntCallback);

        // when - shutdown notification center
        final FutureCallback<Void> shutdownCallback = mock(FutureCallback.class);
        notificationCenter.shutdown(shutdownCallback);

        // when - request notification while shutting down
        final FutureCallback<Void> rqntCallback2 = mock(FutureCallback.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt2, rqntCallback2);

        // then - second RQNT is denied
        verify(rqntCallback2, timeout(50)).onFailure(any(IllegalStateException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRaiseQuarantinedEvent() {
        // given
        final int transactionId = 12345;
        final String requestId = "555";
        final NotifiedEntity notifiedEntity = new NotifiedEntity();

        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[]{requestedEvent1, requestedEvent2};

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        when(timeoutSignal1.getRequestId()).thenReturn(requestId);
        when(timeoutSignal1.getName()).thenReturn("pa");

        final MgcpEvent mgcpEvent = mock(MgcpEvent.class);

        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[]{timeoutSignal1};

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);

        final NotificationCenterContext context = new NotificationCenterContext();
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);

        context.setEndpointId(endpointId.toString());

        notificationCenter.observe(endpoint);

        // when - submit rqnt
        final FutureCallback<Void> callback1 = mock(FutureCallback.class);
        final NotificationRequest rqnt1 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);

        notificationCenter.requestNotification(rqnt1, callback1);

        // then
        verify(callback1, timeout(50)).onSuccess(null);

        // when - override signals
        final FutureCallback<Void> callback2 = mock(FutureCallback.class);
        final NotificationRequest rqnt2 = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, new MgcpSignal<?>[0]);

        notificationCenter.requestNotification(rqnt2, callback2);

        // then
        verify(callback2, timeout(50)).onSuccess(null);
        verify(timeoutSignal1).cancel(any(TimeoutSignalCancellationCallback.class));

        assertTrue(context.getPendingBriefSignals().isEmpty());
        assertTrue(context.getTimeoutSignals().isEmpty());
        assertNull(context.getActiveBriefSignal());

        // when - Raise quarantined event
        final FutureCallback<MgcpEvent> callback3 = mock(FutureCallback.class);
        notificationCenter.endSignal(requestId, timeoutSignal1.getName(), callback3);
        context.getQuarantine().onSignalCompleted(timeoutSignal1, mgcpEvent);

        // then
        verify(callback3).onSuccess(any(MgcpEvent.class));
    }

}
