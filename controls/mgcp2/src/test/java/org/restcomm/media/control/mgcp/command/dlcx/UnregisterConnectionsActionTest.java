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

package org.restcomm.media.control.mgcp.command.dlcx;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterConnectionsActionTest {

    @Test
    public void testUnregisterAllConnections() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(DeleteConnectionContext.NO_CALL_ID);
        context.setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionsCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionsCallback.class);
        verify(endpoint).unregisterConnections(callbackCaptor.capture());

        // when
        final UnregisterConnectionsCallback callback = callbackCaptor.getValue();
        final MgcpConnection[] unregistered = new MgcpConnection[0];
        callback.onSuccess(unregistered);

        // then
        verify(context).setUnregisteredConnections(unregistered);
        verify(fsm).fire(DeleteConnectionEvent.UNREGISTERED_CONNECTIONS, context);
    }

    @Test
    public void testUnregisterAllConnectionsFailure() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(DeleteConnectionContext.NO_CALL_ID);
        context.setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionsCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionsCallback.class);
        verify(endpoint).unregisterConnections(callbackCaptor.capture());

        // when
        final UnregisterConnectionsCallback callback = callbackCaptor.getValue();
        final Exception e = new Exception("test purposes");
        callback.onFailure(e);

        // then
        verify(context, never()).setUnregisteredConnections(any(MgcpConnection[].class));
        verify(context).setError(e);
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

    @Test
    public void testUnregisterCallConnections() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        int callId = 7;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(callId);
        context.setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionsCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionsCallback.class);
        verify(endpoint).unregisterConnections(eq(callId), callbackCaptor.capture());

        // when
        final UnregisterConnectionsCallback callback = callbackCaptor.getValue();
        final MgcpConnection[] unregistered = new MgcpConnection[0];
        callback.onSuccess(unregistered);

        // then
        verify(context).setUnregisteredConnections(unregistered);
        verify(fsm).fire(DeleteConnectionEvent.UNREGISTERED_CONNECTIONS, context);
    }

    @Test
    public void testUnregisterCallConnectionsFailure() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        final int callId = 7;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(callId);
        context.setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionsCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionsCallback.class);
        verify(endpoint).unregisterConnections(eq(callId), callbackCaptor.capture());

        // when
        final UnregisterConnectionsCallback callback = callbackCaptor.getValue();
        final Exception e = new Exception("test purposes");
        callback.onFailure(e);

        // then
        verify(context, never()).setUnregisteredConnections(any(MgcpConnection[].class));
        verify(context).setError(e);
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

    @Test
    public void testUnregisterCallConnectionsWithUnknownCallId() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 7;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        context.setCallId(callId);
        context.setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);
        
        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);
        
        // then
        final ArgumentCaptor<UnregisterConnectionsCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionsCallback.class);
        verify(endpoint).unregisterConnections(eq(callId), callbackCaptor.capture());
        
        // when
        final UnregisterConnectionsCallback callback = callbackCaptor.getValue();
        final MgcpCallNotFoundException e = new MgcpCallNotFoundException("test purposes");
        callback.onFailure(e);
        
        // then
        verify(context, never()).setError(e);
        verify(fsm).fire(DeleteConnectionEvent.UNREGISTERED_CONNECTIONS, context);
    }

    @Test
    public void testUnregisterSingleConnection() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        final int callId = 7;
        final int connectionId = 3;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(callId);
        context.setConnectionId(connectionId);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionCallback.class);
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), callbackCaptor.capture());

        // when
        final UnregisterConnectionCallback callback = callbackCaptor.getValue();
        final MgcpConnection unregistered = mock(MgcpConnection.class);
        callback.onSuccess(unregistered);

        // then
        ArgumentCaptor<MgcpConnection[]> connectionCaptor = ArgumentCaptor.forClass(MgcpConnection[].class);
        verify(context).setUnregisteredConnections(connectionCaptor.capture());
        verify(context).setConnectionParams(any(String.class));
        verify(fsm).fire(DeleteConnectionEvent.UNREGISTERED_CONNECTIONS, context);

        MgcpConnection[] connections = connectionCaptor.getValue();
        assertEquals(1, connections.length);
        assertEquals(unregistered, connections[0]);
    }

    @Test
    public void testUnregisterSingleConnectionFailure() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);

        final int callId = 7;
        final int connectionId = 3;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        context.setCallId(callId);
        context.setConnectionId(connectionId);
        context.setEndpointId(endpointId);
        context.setEndpoint(endpoint);

        // when
        final UnregisterConnectionsAction action = new UnregisterConnectionsAction();
        action.execute(DeleteConnectionState.EXECUTING, DeleteConnectionState.UNREGISTERING_CONNECTIONS, DeleteConnectionEvent.VALIDATED_PARAMETERS, context, fsm);

        // then
        final ArgumentCaptor<UnregisterConnectionCallback> callbackCaptor = ArgumentCaptor.forClass(UnregisterConnectionCallback.class);
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), callbackCaptor.capture());

        // when
        final UnregisterConnectionCallback callback = callbackCaptor.getValue();
        final Exception e = new Exception("test purposes");
        callback.onFailure(e);

        // then
        verify(context, never()).setUnregisteredConnections(any(MgcpConnection[].class));
        verify(context).setError(e);
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

}
