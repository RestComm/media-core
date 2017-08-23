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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterAllConnectionsActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisterAllConnections() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, connectionProvider, mediaGroup);
        final Map<Integer, MgcpConnection> connections = context.getConnections();

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final int callId1 = 5;
        final int connectionId1 = 1;
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        when(connection1.getIdentifier()).thenReturn(connectionId1);
        when(connection1.getCallIdentifier()).thenReturn(callId1);
        when(connection1.isLocal()).thenReturn(false);

        final int connectionId2 = 2;
        final MgcpConnection connection2 = mock(MgcpConnection.class);
        when(connection2.getIdentifier()).thenReturn(connectionId2);
        when(connection2.getCallIdentifier()).thenReturn(callId1);
        when(connection2.isLocal()).thenReturn(false);

        final int callId2 = 8;
        final int connectionId3 = 6;
        final MgcpConnection connection3 = mock(MgcpConnection.class);
        when(connection3.getIdentifier()).thenReturn(connectionId3);
        when(connection3.getCallIdentifier()).thenReturn(callId2);
        when(connection3.isLocal()).thenReturn(true);

        connections.put(connection1.getIdentifier(), connection1);
        connections.put(connection2.getIdentifier(), connection2);
        connections.put(connection3.getIdentifier(), connection3);

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<MgcpConnection[]> callback = mock(FutureCallback.class);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        UnregisterAllConnectionsAction action = new UnregisterAllConnectionsAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTER_CONNECTION, txContext, fsm);

        // then
        assertTrue(connections.isEmpty());
        MgcpConnection[] unregistered = txContext.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);
        assertNotNull(unregistered);
        assertEquals(3, unregistered.length);
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNotNull(connectionCount);
        assertEquals(0, connectionCount.intValue());
        verify(connection1).forget(observer);
        verify(connection2).forget(observer);
        verify(connection3).forget(observer);
        final ArgumentCaptor<MgcpConnection[]> captor = ArgumentCaptor.forClass(MgcpConnection[].class);
        verify(callback).onSuccess(captor.capture());
        assertEquals(3, captor.getValue().length);
        verify(fsm).fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregisterAllConnectionsWhenIdle() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, connectionProvider, mediaGroup);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<MgcpConnection[]> callback = mock(FutureCallback.class);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        UnregisterAllConnectionsAction action = new UnregisterAllConnectionsAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTER_CONNECTION, txContext, fsm);

        // then
        MgcpConnection[] unregistered = txContext.get(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, MgcpConnection[].class);
        assertNull(unregistered);
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNull(connectionCount);
        final ArgumentCaptor<MgcpConnection[]> captor = ArgumentCaptor.forClass(MgcpConnection[].class);
        verify(callback).onSuccess(captor.capture());
        assertEquals(0, captor.getValue().length);
        verify(fsm, never()).fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext);
    }

}
