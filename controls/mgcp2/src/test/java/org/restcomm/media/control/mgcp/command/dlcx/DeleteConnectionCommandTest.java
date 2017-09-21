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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionCommandTest {
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteSingleConnection() {
        // given
        final int transactionId = 12345;
        final int callId = 5;
        final int connectionId = 8;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";

        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection = mock(MgcpConnection.class);

        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);

        doAnswer(new Answer<MgcpConnection>() {

            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionCallback callback = invocation.getArgumentAt(2, UnregisterConnectionCallback.class);
                callback.onSuccess(connection);
                return null;
            }
            
        }).when(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection).close(any(CloseConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        verify(connection).close(any(CloseConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertTrue(responseParameters.containsKey(MgcpParameterType.CONNECTION_PARAMETERS));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteSingleConnectionWithUnknownConnectionId() {
        // given
        final int transactionId = 12345;
        final int callId = 5;
        final int connectionId = 8;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        doAnswer(new Answer<MgcpConnection>() {
            
            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionCallback callback = invocation.getArgumentAt(2, UnregisterConnectionCallback.class);
                final MgcpConnectionNotFoundException e = new MgcpConnectionNotFoundException("test purposes");
                callback.onFailure(e);
                return null;
            }
            
        }).when(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), result.getCode());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteSingleConnectionWithUnknownCallId() {
        // given
        final int transactionId = 12345;
        final int callId = 5;
        final int connectionId = 8;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        doAnswer(new Answer<MgcpConnection>() {
            
            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionCallback callback = invocation.getArgumentAt(2, UnregisterConnectionCallback.class);
                final MgcpCallNotFoundException e = new MgcpCallNotFoundException("test purposes");
                callback.onFailure(e);
                return null;
            }
            
        }).when(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnection(eq(callId), eq(connectionId), any(UnregisterConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), result.getCode());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteCallConnections() {
        // given
        final int transactionId = 12345;
        final int callId = 5;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";

        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        final MgcpConnection connection2 = mock(MgcpConnection.class);

        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);

        doAnswer(new Answer<MgcpConnection>() {

            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionsCallback callback = invocation.getArgumentAt(1, UnregisterConnectionsCallback.class);
                callback.onSuccess(new MgcpConnection[] { connection1, connection2 });
                return null;
            }
            
        }).when(endpoint).unregisterConnections(eq(callId), any(UnregisterConnectionsCallback.class));
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection1).close(any(CloseConnectionCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection2).close(any(CloseConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnections(eq(callId), any(UnregisterConnectionsCallback.class));
        verify(connection1).close(any(CloseConnectionCallback.class));
        verify(connection2).close(any(CloseConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertTrue(responseParameters.containsKey(MgcpParameterType.CONNECTION_PARAMETERS));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteCallConnectionsWithUnknownCallId() {
        // given
        final int transactionId = 12345;
        final int callId = 5;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        final MgcpConnection connection2 = mock(MgcpConnection.class);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        doAnswer(new Answer<MgcpConnection>() {
            
            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionsCallback callback = invocation.getArgumentAt(1, UnregisterConnectionsCallback.class);
                callback.onSuccess(new MgcpConnection[] { connection1, connection2 });
                return null;
            }
            
        }).when(endpoint).unregisterConnections(eq(callId), any(UnregisterConnectionsCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection1).close(any(CloseConnectionCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection2).close(any(CloseConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnections(eq(callId), any(UnregisterConnectionsCallback.class));
        verify(connection1).close(any(CloseConnectionCallback.class));
        verify(connection2).close(any(CloseConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertTrue(responseParameters.containsKey(MgcpParameterType.CONNECTION_PARAMETERS));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteAllConnections() {
        // given
        final int transactionId = 12345;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        
        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        final MgcpConnection connection2 = mock(MgcpConnection.class);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        doAnswer(new Answer<MgcpConnection>() {
            
            @Override
            public MgcpConnection answer(InvocationOnMock invocation) throws Throwable {
                final UnregisterConnectionsCallback callback = invocation.getArgumentAt(0, UnregisterConnectionsCallback.class);
                callback.onSuccess(new MgcpConnection[] { connection1, connection2 });
                return null;
            }
            
        }).when(endpoint).unregisterConnections(any(UnregisterConnectionsCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection1).close(any(CloseConnectionCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final CloseConnectionCallback callback = invocation.getArgumentAt(0, CloseConnectionCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection2).close(any(CloseConnectionCallback.class));
        
        final DeleteConnectionContext context = new DeleteConnectionContext(transactionId, parameters, endpointManager);
        final DeleteConnectionFsm fsm = DeleteConnectionFsmBuilder.INSTANCE.build();
        
        // when
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        dlcx.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        verify(endpoint).unregisterConnections(any(UnregisterConnectionsCallback.class));
        verify(connection1).close(any(CloseConnectionCallback.class));
        verify(connection2).close(any(CloseConnectionCallback.class));
        
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertTrue(responseParameters.containsKey(MgcpParameterType.CONNECTION_PARAMETERS));
    }

}
