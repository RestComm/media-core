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

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.NotificationRequest;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.pkg.MgcpActionType;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.signal.BriefSignal;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.signal.TimeoutSignal;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotificationCenterTest {
    
    private NotificationCenterFsm fsm;
    
    @After
    public void after() {
        if(fsm != null && fsm.isStarted()) {
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
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent1, requestedEvent2 };

        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
        final BriefSignal briefSignal1 = mock(BriefSignal.class);
        final BriefSignal briefSignal2 = mock(BriefSignal.class);
        final BriefSignal briefSignal3 = mock(BriefSignal.class);
        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[] { timeoutSignal1, timeoutSignal2, briefSignal1, briefSignal2, timeoutSignal3, briefSignal3 };

        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final NotificationCenterContext context = new NotificationCenterContext(endpoint);
        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);
        
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
    
//    @Test
//    @SuppressWarnings("unchecked")
//    public void testDeactivationWithOngoingSignals() {
//        // given
//        final int transactionId = 12345;
//        final String requestId = "555";
//        final NotifiedEntity notifiedEntity = new NotifiedEntity();
//        
//        final MgcpRequestedEvent requestedEvent1 = new MgcpRequestedEvent(requestId, "AU", "oc", MgcpActionType.NOTIFY);
//        final MgcpRequestedEvent requestedEvent2 = new MgcpRequestedEvent(requestId, "AU", "of", MgcpActionType.NOTIFY);
//        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { requestedEvent1, requestedEvent2 };
//        
//        final TimeoutSignal timeoutSignal1 = mock(TimeoutSignal.class);
//        final TimeoutSignal timeoutSignal2 = mock(TimeoutSignal.class);
//        final TimeoutSignal timeoutSignal3 = mock(TimeoutSignal.class);
//        final BriefSignal briefSignal1 = mock(BriefSignal.class);
//        final BriefSignal briefSignal2 = mock(BriefSignal.class);
//        final BriefSignal briefSignal3 = mock(BriefSignal.class);
//        final MgcpSignal<?>[] requestedSignals = new MgcpSignal[] { timeoutSignal1, timeoutSignal2, briefSignal1, briefSignal2, timeoutSignal3, briefSignal3 };
//        
//        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
//        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
//        when(endpoint.getEndpointId()).thenReturn(endpointId);
//        
//        final NotificationCenterContext context = new NotificationCenterContext(endpoint);
//        this.fsm = NotificationCenterFsmBuilder.INSTANCE.build(context);
//        final NotificationCenterImpl notificationCenter = new NotificationCenterImpl(this.fsm);
//        
//        // when - submit rqnt
//        final FutureCallback<Void> rqntCallback = mock(FutureCallback.class);
//        final NotificationRequest rqnt = new NotificationRequest(transactionId, requestId, notifiedEntity, requestedEvents, requestedSignals);
//        
//        notificationCenter.requestNotification(rqnt, rqntCallback);
//        verify(rqntCallback, timeout(50)).onSuccess(null);
//        
//        final FutureCallback<Void> shutdownCallback = mock(FutureCallback.class);
//        notificationCenter.shutdown(shutdownCallback);
//        
//        // then
//        verify(briefSignal2).execute(any(BriefSignalExecutionCallback.class));
//        verify(briefSignal3, never()).execute(any(BriefSignalExecutionCallback.class));
//        assertEquals(1, context.getPendingBriefSignals().size());
//        
//        // when - TO signal times out
//        final MgcpEvent event = mock(MgcpEvent.class);
//        when(event.getPackage()).thenReturn("AU");
//        when(event.getSymbol()).thenReturn("oc");
//        
//        timeoutCallbackCaptor.getValue().onSuccess(event);
//        
//        // then
//        verify(timeoutSignal2).cancel(any(TimeoutSignalCancellationCallback.class));
//        verify(timeoutSignal3).cancel(any(TimeoutSignalCancellationCallback.class));
//        verify(endpoint).onEvent(any(), eq(event));
//        
//        assertTrue(context.getPendingBriefSignals().isEmpty());
//        assertTrue(context.getTimeoutSignals().isEmpty());
//    }

}
