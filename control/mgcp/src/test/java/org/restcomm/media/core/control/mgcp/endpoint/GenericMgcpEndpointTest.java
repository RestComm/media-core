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

package org.restcomm.media.core.control.mgcp.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.core.control.mgcp.command.NotificationRequest;
import org.restcomm.media.core.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.core.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.core.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.core.control.mgcp.connection.MgcpLocalConnection;
import org.restcomm.media.core.control.mgcp.connection.MgcpRemoteConnection;
import org.restcomm.media.core.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.core.control.mgcp.endpoint.GenericMgcpEndpoint;
import org.restcomm.media.core.control.mgcp.endpoint.MediaGroup;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpEndpointObserver;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.core.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.core.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.core.control.mgcp.message.MessageDirection;
import org.restcomm.media.core.control.mgcp.message.MgcpMessage;
import org.restcomm.media.core.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.core.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.core.control.mgcp.message.MgcpRequest;
import org.restcomm.media.core.control.mgcp.pkg.AbstractMgcpSignal;
import org.restcomm.media.core.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.core.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.core.control.mgcp.pkg.MgcpSignal;
import org.restcomm.media.core.control.mgcp.pkg.SignalType;
import org.restcomm.media.core.control.mgcp.pkg.r.rto.RtpTimeoutEvent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEndpointTest {

    @Test
    public void testCreateConnection() {
        // given
        final int callId1 = 1;
        final int callId2 = 2;
        final int connectionId1 = 3;
        final int connectionId2 = 4;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpRemoteConnection connection2 = mock(MgcpRemoteConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connectionProvider.provideRemote(callId2)).thenReturn(connection2);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);
        when(connection2.getIdentifier()).thenReturn(connectionId2);
        when(connection2.getCallIdentifier()).thenReturn(callId2);

        final MgcpConnection created1 = endpoint.createConnection(callId1, true);
        final MgcpConnection created2 = endpoint.createConnection(callId2, false);

        // then
        assertTrue(endpoint.hasConnections());
        assertEquals(connection1, created1);
        assertEquals(connection2, created2);
    }

    @Test
    public void testDeleteConnectionFromCall() throws Exception {
        // given
        final int callId1 = 1;
        final int callId2 = 2;
        final int connectionId1 = 3;
        final int connectionId2 = 4;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpRemoteConnection connection2 = mock(MgcpRemoteConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connectionProvider.provideRemote(callId2)).thenReturn(connection2);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);
        when(connection2.getIdentifier()).thenReturn(connectionId2);
        when(connection2.getCallIdentifier()).thenReturn(callId2);

        endpoint.createConnection(callId1, true);
        endpoint.createConnection(callId2, false);
        MgcpConnection deleted = endpoint.deleteConnection(callId2, connectionId2);
        MgcpConnection existing = endpoint.getConnection(callId1, connectionId1);

        // then
        assertTrue(endpoint.hasConnections());
        assertEquals(connection2, deleted);
        assertEquals(connection1, existing);
    }

    @Test(expected = MgcpCallNotFoundException.class)
    public void testDeleteConnectionFromInexistentCall() throws Exception {
        // given
        final int callId1 = 1;
        final int callId2 = 2;
        final int connectionId1 = 3;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);

        endpoint.createConnection(callId1, true);
        endpoint.deleteConnection(callId2, connectionId1);
    }

    @Test(expected = MgcpConnectionNotFoundException.class)
    public void testDeleteInexistentConnectionFromCall() throws Exception {
        // given
        final int callId1 = 1;
        final int connectionId1 = 3;
        final int connectionId2 = 4;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);

        endpoint.createConnection(callId1, true);
        endpoint.deleteConnection(callId1, connectionId2);
    }

    @Test
    public void testDeleteConnectionsFromCall() throws Exception {
        // given
        final int callId1 = 1;
        final int callId2 = 2;
        final int connectionId1 = 3;
        final int connectionId2 = 4;
        final int connectionId3 = 5;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpRemoteConnection connection2 = mock(MgcpRemoteConnection.class);
        final MgcpRemoteConnection connection3 = mock(MgcpRemoteConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connectionProvider.provideRemote(callId2)).thenReturn(connection2, connection3);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);
        when(connection2.getIdentifier()).thenReturn(connectionId2);
        when(connection2.getCallIdentifier()).thenReturn(callId2);
        when(connection3.getIdentifier()).thenReturn(connectionId3);
        when(connection3.getCallIdentifier()).thenReturn(callId2);

        endpoint.createConnection(callId1, true);
        endpoint.createConnection(callId2, false);
        endpoint.createConnection(callId2, false);

        List<MgcpConnection> deleted = endpoint.deleteConnections(callId2);
        MgcpConnection existing = endpoint.getConnection(callId1, connectionId1);

        // then
        assertTrue(endpoint.hasConnections());
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains(connection2));
        assertTrue(deleted.contains(connection3));
        assertEquals(connection1, existing);
    }

    @Test
    public void testDeleteConnections() throws Exception {
        // given
        final int callId1 = 1;
        final int callId2 = 2;
        final int connectionId1 = 3;
        final int connectionId2 = 4;
        final int connectionId3 = 5;
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpRemoteConnection connection2 = mock(MgcpRemoteConnection.class);
        final MgcpRemoteConnection connection3 = mock(MgcpRemoteConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(connectionProvider.provideLocal(callId1)).thenReturn(connection1);
        when(connectionProvider.provideRemote(callId2)).thenReturn(connection2, connection3);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);
        when(connection2.getIdentifier()).thenReturn(connectionId2);
        when(connection2.getCallIdentifier()).thenReturn(callId2);
        when(connection3.getIdentifier()).thenReturn(connectionId3);
        when(connection3.getCallIdentifier()).thenReturn(callId2);

        endpoint.createConnection(callId1, true);
        endpoint.createConnection(callId2, false);
        endpoint.createConnection(callId2, false);

        List<MgcpConnection> deleted = endpoint.deleteConnections();

        // then
        assertFalse(endpoint.hasConnections());
        assertEquals(3, deleted.size());
        assertTrue(deleted.contains(connection1));
        assertTrue(deleted.contains(connection2));
        assertTrue(deleted.contains(connection3));
    }

    @Test
    public void testDeleteConnectionsFromInactiveEndpoint() throws Exception {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final GenericMgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        List<MgcpConnection> deleted = endpoint.deleteConnections();

        // then
        assertNotNull(deleted);
        assertTrue(deleted.isEmpty());
    }

    @Test
    public void testExecuteTimeoutSignal() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };

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
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
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
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
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
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
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
    public void testCancelOngoingSignalsOnDeactivation() throws Exception {
        // given
        final int callId = 1;
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class), mock(MgcpRequestedEvent.class) };
        final MgcpSignal signal1 = mock(MgcpSignal.class);
        final MgcpSignal signal2 = mock(MgcpSignal.class);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1, signal2);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int connectionId = 7;
        final MgcpLocalConnection connection = mock(MgcpLocalConnection.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);
        
        // when
        when(connectionProvider.provideLocal(callId)).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.getCallIdentifier()).thenReturn(callId);
        
        when(signal1.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal1.getName()).thenReturn("AU/pa");
        when(signal2.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal2.getName()).thenReturn("AU/pc");
        
        genericMgcpEndpoint.createConnection(callId, true);
        genericMgcpEndpoint.requestNotification(rqnt1);
        
        // then
        verify(signal1, times(1)).execute();
        verify(signal2, times(1)).execute();
        verify(signal1, times(0)).cancel();
        verify(signal2, times(0)).cancel();

        // when
        genericMgcpEndpoint.deleteConnection(callId, connectionId);
        
        // then
        verify(signal1, times(1)).cancel();
        verify(signal2, times(1)).cancel();
    }

    @Test
    public void testExecuteTimeoutSignalDuringSignalExecution() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
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
    public void testExecuteTimeoutSignalWhileEquivalentSignalIsExecuting() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
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
    public void testExecuteTimeoutSignalWhileInequivalentSignalIsExecuting() {
        // given
        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);
        final MgcpRequestedEvent[] requestedEvents = new MgcpRequestedEvent[] { mock(MgcpRequestedEvent.class),
                mock(MgcpRequestedEvent.class) };
        final Map<String, String> signal1Parameters = new HashMap<>(1);
        final Map<String, String> signal2Parameters = new HashMap<>(1);
        final MockSignal signal1 = new MockSignal("AU", "pa", SignalType.TIME_OUT, 1, signal1Parameters);
        final MockSignal signal2 = new MockSignal("AU", "pa", SignalType.TIME_OUT, 2, signal2Parameters);
        final NotificationRequest rqnt1 = new NotificationRequest(1, "1a", notifiedEntity, requestedEvents, signal1);
        final NotificationRequest rqnt2 = new NotificationRequest(2, "1b", notifiedEntity, requestedEvents, signal2);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint genericMgcpEndpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        signal1Parameters.put("prompt", "promtpX");
        signal2Parameters.put("prompt", "promtpY");
        genericMgcpEndpoint.requestNotification(rqnt1);
        genericMgcpEndpoint.requestNotification(rqnt2);

        // then
        assertTrue(signal1.calledExecute);
        assertTrue(signal1.calledCancel);
        assertTrue(signal2.calledExecute);
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
        verify(msgObserver, timeout(5)).onMessage(eq(localAddress), eq(remoteAddress), eventCaptor.capture(),
                eq(MessageDirection.OUTGOING));

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
        verify(msgObserver, timeout(5)).onMessage(eq(localAddress), eq(remoteAddress), eventCaptor.capture(),
                eq(MessageDirection.OUTGOING));

        final MgcpMessage ntfy = eventCaptor.getValue();
        assertTrue(ntfy instanceof MgcpRequest);
        assertEquals(notifiedEntity.toString(), ntfy.getParameter(MgcpParameterType.NOTIFIED_ENTITY));
    }

    @Test
    public void testExecuteTimeoutSignalAndDeleteConnection() {
        // given
        final int callId = 3;
        final int connectionId = 5;

        final MgcpMessageObserver msgObserver = mock(MgcpMessageObserver.class);
        final MgcpEndpointObserver endpointObserver = mock(MgcpEndpointObserver.class);

        final NotifiedEntity notifiedEntity = new NotifiedEntity("call-agent", "127.0.0.1", 2727);

        final MgcpSignal signal = mock(MgcpSignal.class);
        final MgcpEvent timeoutEvent = new RtpTimeoutEvent(1, 10);
        final MgcpLocalConnection connection = mock(MgcpLocalConnection.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/endpoint/1", "127.0.0.1");
        final MgcpEndpoint endpoint = new GenericMgcpEndpoint(endpointId, connectionProvider, mediaGroup);

        // when
        when(signal.getSignalType()).thenReturn(SignalType.TIME_OUT);
        when(signal.getNotifiedEntity()).thenReturn(notifiedEntity);
        when(signal.getName()).thenReturn("AU/pa");

        when(connectionProvider.provideLocal(callId)).thenReturn(connection);
        when(connection.getCallIdentifier()).thenReturn(callId);
        when(connection.getIdentifier()).thenReturn(connectionId);

        endpoint.observe(endpointObserver);
        endpoint.observe(msgObserver);
        final MgcpConnection createdConnection = endpoint.createConnection(callId, true);

        // then
        assertEquals(connection, createdConnection);
        assertEquals(connection, endpoint.getConnection(callId, connectionId));

        // when
        endpoint.onEvent(connection, timeoutEvent);

        // then
        assertNull(endpoint.getConnection(callId, connectionId));
        verify(msgObserver, never()).onMessage(any(InetSocketAddress.class), any(InetSocketAddress.class), any(MgcpMessage.class), any(MessageDirection.class));
        verify(endpointObserver, times(1)).onEndpointStateChanged(endpoint, MgcpEndpointState.INACTIVE);
    }

    /**
     * Needed to create a mock class because Mockito overrides equals() so we cannot use mocks for MgcpSignal.
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    private class MockSignal extends AbstractMgcpSignal {

        boolean calledExecute = false;
        boolean calledCancel = false;

        public MockSignal(String packageName, String symbol, SignalType type, int requestId) {
            super(packageName, symbol, type, requestId);
        }

        public MockSignal(String packageName, String symbol, SignalType type, int requestId, Map<String, String> parameters) {
            super(packageName, symbol, type, requestId, parameters);
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
