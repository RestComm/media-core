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

package org.restcomm.media.control.mgcp.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterConnectionActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisterConnection() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        final Map<Integer, MgcpConnection> connections = context.getConnections();

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final int connectionId = 1;
        final int callId = 5;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.getCallIdentifier()).thenReturn(callId);
        when(connection.isLocal()).thenReturn(false);

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<MgcpConnection> callback = mock(FutureCallback.class);

        connections.put(connection.getIdentifier(), connection);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.CALL_ID, callId);
        txContext.set(MgcpEndpointParameter.CONNECTION_ID, connectionId);
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        UnregisterConnectionAction action = new UnregisterConnectionAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTER_CONNECTION, txContext, fsm);

        // then
        assertFalse(connections.containsKey(connectionId));
        MgcpConnection[] unregistered = txContext.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);
        assertNotNull(unregistered);
        assertEquals(1, unregistered.length);
        assertEquals(connection, unregistered[0]);
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNotNull(connectionCount);
        assertEquals(connections.size(), connectionCount.intValue());
        verify(connection).forget(observer);
        verify(callback).onSuccess(connection);
        verify(fsm).fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisterUnknownConnection() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final int connectionId = 1;
        final int callId = 5;

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<MgcpConnection> callback = mock(FutureCallback.class);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.CALL_ID, callId);
        txContext.set(MgcpEndpointParameter.CONNECTION_ID, connectionId);
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        UnregisterConnectionAction action = new UnregisterConnectionAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTER_CONNECTION, txContext, fsm);

        // then
        MgcpConnection[] unregistered = txContext.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);
        assertNull(unregistered);
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNull(connectionCount);
        verify(callback).onFailure(any(MgcpConnectionNotFoundException.class));
        verify(fsm, never()).fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisterConnectionWithUnknownCallId() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        Map<Integer, MgcpConnection> connections = context.getConnections();

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final int connectionId = 1;
        final int callId = 5;
        final int unknownCallId = 6;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.getCallIdentifier()).thenReturn(callId);
        when(connection.isLocal()).thenReturn(false);

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<MgcpConnection> callback = mock(FutureCallback.class);

        connections.put(connection.getIdentifier(), connection);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.CALL_ID, unknownCallId);
        txContext.set(MgcpEndpointParameter.CONNECTION_ID, connectionId);
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        UnregisterConnectionAction action = new UnregisterConnectionAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTER_CONNECTION, txContext, fsm);

        // then
        assertTrue(connections.containsKey(connectionId));
        MgcpConnection[] unregistered = txContext.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);
        assertNull(unregistered);
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNull(connectionCount);
        verify(connection, never()).forget(any(MgcpEventObserver.class));
        verify(callback).onFailure(any(MgcpCallNotFoundException.class));
        verify(fsm, never()).fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext);
    }

}
