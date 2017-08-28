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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.exception.DuplicateMgcpConnectionException;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RegisterConnectionActionTest {

    @Test
    public void testRegisterNewConnection() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final int connectionId = 1;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        when(connection.isLocal()).thenReturn(false);

        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<?> callback = mock(FutureCallback.class);
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.REGISTERED_CONNECTION, connection);
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);

        // when
        RegisterConnectionAction action = new RegisterConnectionAction();
        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTER_CONNECTION, txContext, fsm);

        // then
        Map<Integer, MgcpConnection> connections = context.getConnections();
        assertEquals(1, connections.size());
        assertEquals(connection, connections.get(connectionId));
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNotNull(connectionCount);
        assertEquals(connections.size(), connectionCount.intValue());
        verify(connection).observe(observer);
        verify(callback).onSuccess(null);
    }

    @Test
    public void testRegisterExistingConnection() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        
        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final int connectionId = 1;
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);
        context.getConnections().put(connection.getIdentifier(), connection);
        
        final MgcpEventObserver observer = mock(MgcpEventObserver.class);
        final FutureCallback<?> callback = mock(FutureCallback.class);
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.REGISTERED_CONNECTION, connection);
        txContext.set(MgcpEndpointParameter.EVENT_OBSERVER, observer);
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);
        
        // when
        RegisterConnectionAction action = new RegisterConnectionAction();
        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTER_CONNECTION, txContext, fsm);
        
        // then
        Map<Integer, MgcpConnection> connections = context.getConnections();
        assertEquals(1, connections.size());
        Integer connectionCount = txContext.get(MgcpEndpointParameter.CONNECTION_COUNT, Integer.class);
        assertNull(connectionCount);
        verify(connection, never()).observe(observer);
        verify(callback).onFailure(any(DuplicateMgcpConnectionException.class));
    }

}
