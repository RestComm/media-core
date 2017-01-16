/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.NotificationRequest;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.pkg.AbstractMgcpSignal;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;
import org.mockito.ArgumentCaptor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEndpointTest {

    @Test
    public void testExecuteTimeoutSignal() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        
        final MgcpSignal signal = mock(MgcpSignal.class);
        final NotificationRequest rqnt = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(signal.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal.getName()).thenReturn("AU/pa");
        genericMgcpEndpoint.requestNotification(rqnt);

        // then
        verify(signal, times(1)).execute();
    }

    @Test
    public void testExecuteMultipleTimeoutSignals() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MgcpSignal signal1 = mock(MgcpSignal.class);
        final MgcpSignal signal2 = mock(MgcpSignal.class);
        final NotificationRequest rqnt = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1, signal2);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        when(signal1.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal1.getName()).thenReturn("AU/pa");
        when(signal2.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal2.getName()).thenReturn("AU/pc");

        genericMgcpEndpoint.requestNotification(rqnt);
        
        // then
        verify(signal1, times(1)).execute();
        verify(signal2, times(1)).execute();
    }

    @Test
    public void testOverriedExecutingSignals() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MgcpSignal signal1 = mock(MgcpSignal.class);
        final MgcpSignal signal2 = mock(MgcpSignal.class);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1, signal2);
        final MgcpSignal signal3 = mock(MgcpSignal.class);
        final NotificationRequest rqnt2 = new NotificationRequest(2, "1b", notifiedEntity, requestedEvents, signal1, signal3);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        when(signal1.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal1.getName()).thenReturn("AU/pa");
        when(signal2.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal2.getName()).thenReturn("AU/pc");
        when(signal3.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal3.getName()).thenReturn("AU/pr");
        
        genericMgcpEndpoint.requestNotification(rqnt1);
        genericMgcpEndpoint.requestNotification(rqnt2);
        
        // then
        verify(signal1, times(1)).execute();
        verify(signal1, times(0)).cancel();
        verify(signal2, times(1)).execute();
        verify(signal2, times(1)).cancel();
        verify(signal3, times(1)).execute();
        verify(signal3, times(0)).cancel();
    }

    @Test
    public void testCancelAllExecutingSignals() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MgcpSignal signal1 = mock(MgcpSignal.class);
        final MgcpSignal signal2 = mock(MgcpSignal.class);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1, signal2);
        final NotificationRequest rqnt2 = new NotificationRequest(2, "1b", notifiedEntity, requestedEvents);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        when(signal1.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal1.getName()).thenReturn("AU/pa");
        when(signal2.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal2.getName()).thenReturn("AU/pc");
        
        genericMgcpEndpoint.requestNotification(rqnt1);
        genericMgcpEndpoint.requestNotification(rqnt2);
        
        // then
        verify(signal1, times(1)).execute();
        verify(signal1, times(1)).cancel();
        verify(signal2, times(1)).execute();
        verify(signal2, times(1)).cancel();
    }

    @Test
    public void testExecuteTimeoutSignalDuringSignalExecution() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MockSignal signal1 = new MockSignal("AU", "pa", SignalType.TIME_OUT, 1);
        final MockSignal signal2 = new MockSignal("AU", "pc", SignalType.TIME_OUT, 2);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1);
        final NotificationRequest rqnt2 = new NotificationRequest(2, "1b", notifiedEntity, requestedEvents, signal2);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        genericMgcpEndpoint.requestNotification(rqnt1);
        genericMgcpEndpoint.requestNotification(rqnt2);

        // then
        assertTrue(signal1.calledExecute);
        assertTrue(signal1.calledCancel);
        assertTrue(signal2.calledExecute);
        assertFalse(signal2.calledCancel);
    }

    @Test
    public void testExecuteTimeoutSignalWhileSameSignalTypeIsExecuting() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MockSignal signal1 = new MockSignal("AU", "pa", SignalType.TIME_OUT, 1);
        final MockSignal signal2 = new MockSignal("AU", "pa", SignalType.TIME_OUT, 2);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1);
        final NotificationRequest rqnt2 = new NotificationRequest(2, "1b", notifiedEntity, requestedEvents, signal2);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        genericMgcpEndpoint.requestNotification(rqnt1);
        genericMgcpEndpoint.requestNotification(rqnt2);
        
        // then
        assertTrue(signal1.calledExecute);
        assertFalse(signal1.calledCancel);
        assertFalse(signal2.calledExecute);
        assertFalse(signal2.calledCancel);
    }

    @Test
    public void testExecuteTimeoutSignalAndTriggerNotifyActionWithoutNotifiedEntity() {
        // given
        final ArgumentCaptor<MgcpMessage> eventCaptor = ArgumentCaptor.forClass(MgcpMessage.class);
        final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpMessageObserver msgObserver = mock(MgcpMessageObserver.class);
        
        final MgcpRequestedEvent ocEvent = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent ofEvent = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { ocEvent, ofEvent };

        final MgcpSignal signal = mock(MgcpSignal.class);
        final MgcpEvent event = mock(MgcpEvent.class);
        final NotificationRequest rqnt = new NotificationRequest(1, "1a", null, requestedEvents, signal);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(signal.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal.getName()).thenReturn("AU/pa");
        when(signal.getNotifiedEntity()).thenReturn(null);
        when(event.getPackage()).thenReturn("AU");
        when(event.getSymbol()).thenReturn("oc");
        when(ocEvent.getQualifiedName()).thenReturn("AU/oc");
        when(ofEvent.getQualifiedName()).thenReturn("AU/oc");
        
        genericMgcpEndpoint.observe(msgObserver);
        genericMgcpEndpoint.requestNotification(rqnt);
        genericMgcpEndpoint.onEvent(signal, event);
        
        // then
        verify(signal, times(1)).execute();
        verify(msgObserver, timeout(5)).onMessage(eq(localAddress), eq(remoteAddress), eventCaptor.capture(), eq(MessageDirection.OUTGOING));
        
        final MgcpMessage ntfy = eventCaptor.getValue();
        assertTrue(ntfy instanceof MgcpRequest);
        assertEquals(null, ntfy.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
    }

    @Test
    public void testExecuteTimeoutSignalAndTriggerNotifyActionWithNotifiedEntity() {
        // given
        final ArgumentCaptor<MgcpMessage> eventCaptor = ArgumentCaptor.forClass(MgcpMessage.class);
        final InetSocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpMessageObserver msgObserver = mock(MgcpMessageObserver.class);
        
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent ocEvent = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent ofEvent = mock(MgcpRequestedEvent.class);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { ocEvent, ofEvent };
        
        final MgcpSignal signal = mock(MgcpSignal.class);
        final MgcpEvent event = mock(MgcpEvent.class);
        final NotificationRequest rqnt = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        when(signal.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal.getNotifiedEntity()).thenReturn(notifiedEntity);
        when(signal.getName()).thenReturn("AU/pa");
        when(event.getPackage()).thenReturn("AU");
        when(event.getSymbol()).thenReturn("oc");
        when(ocEvent.getQualifiedName()).thenReturn("AU/oc");
        when(ofEvent.getQualifiedName()).thenReturn("AU/oc");
        
        genericMgcpEndpoint.observe(msgObserver);
        genericMgcpEndpoint.requestNotification(rqnt);
        genericMgcpEndpoint.onEvent(signal, event);
        
        // then
        verify(signal, times(1)).execute();
        verify(msgObserver, timeout(5)).onMessage(eq(localAddress), eq(remoteAddress), eventCaptor.capture(), eq(MessageDirection.OUTGOING));
        
        final MgcpMessage ntfy = eventCaptor.getValue();
        assertTrue(ntfy instanceof MgcpRequest);
        assertEquals(notifiedEntity.toString(), ntfy.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
    }

    /**
     * Needed to create a mock class because Mockito overrides equals() so we cannot use mocks for MgcpSignal.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private final class MockSignal extends AbstractMgcpSignal {

        boolean calledExecute = false;
        boolean calledCancel = false;

        public MockSignal(String packageName, String symbol, SignalType type, int requestId) {
            super(packageName, symbol, type, requestId);
        }

        @Override
        public void execute() {
            this.calledExecute = true;
        }

        @Override
        public void cancel() {
            this.calledCancel = true;
        }

        @Override
        protected boolean isParameterSupported(String name) {
            return true;
        }

    }

}
