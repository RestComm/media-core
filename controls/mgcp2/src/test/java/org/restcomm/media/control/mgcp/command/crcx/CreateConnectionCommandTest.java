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

package org.restcomm.media.control.mgcp.command.crcx;

import static org.mockito.Mockito.*;

import org.junit.Test;

import static org.junit.Assert.*;

import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.MgcpLocalConnection;
import org.restcomm.media.control.mgcp.connection.local.MgcpLocalConnectionImpl;
import org.restcomm.media.control.mgcp.connection.remote.MgcpRemoteConnectionImpl;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommandTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateLocalConnectionsBetweenTwoEndpoints() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);

        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(1);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(2);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).join(any(MgcpLocalConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);

        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());

        verify(primaryConnection).open(any(String.class), any(FutureCallback.class));
        verify(primaryConnection).updateMode(eq(ConnectionMode.SEND_ONLY), any(FutureCallback.class));
        verify(primaryConnection).join(eq(secondaryConnection), any(FutureCallback.class));
        verify(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        verify(secondaryConnection).updateMode(eq(ConnectionMode.SEND_ONLY), any(FutureCallback.class));
        verify(primaryEndpoint).registerConnection(eq(primaryConnection), any(FutureCallback.class));
        verify(secondaryEndpoint).registerConnection(eq(secondaryConnection), any(FutureCallback.class));

        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(primaryEndpointId.toString(), result.getParameters().getString(MgcpParameterType.ENDPOINT_ID).get());
        assertEquals(primaryConnection.getHexIdentifier(), result.getParameters().getString(MgcpParameterType.CONNECTION_ID).get());
        assertEquals(secondaryEndpointId.toString(), result.getParameters().getString(MgcpParameterType.SECOND_ENDPOINT).get());
        assertEquals(secondaryConnection.getHexIdentifier(), result.getParameters().getString(MgcpParameterType.CONNECTION_ID2).get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateOutboundRemoteConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        
        final int callId = 1;
        final int transactionId = 147483655;
        final String localSdp = "local-sdp-mock";
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateRemoteConnectionFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpRemoteConnectionImpl primaryConnection = mock(MgcpRemoteConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideRemote(callId)).thenReturn(primaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(1);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }
            
        }).when(primaryConnection).halfOpen(any(LocalConnectionOptions.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).halfOpen(any(LocalConnectionOptions.class), any(FutureCallback.class));
        verify(primaryConnection).updateMode(eq(ConnectionMode.SEND_ONLY), any(FutureCallback.class));
        verify(primaryEndpoint).registerConnection(eq(primaryConnection), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(primaryEndpointId.toString(), result.getParameters().getString(MgcpParameterType.ENDPOINT_ID).get());
        assertEquals(primaryConnection.getHexIdentifier(), result.getParameters().getString(MgcpParameterType.CONNECTION_ID).get());
        assertEquals(localSdp, result.getParameters().getString(MgcpParameterType.SDP).get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCreateInboundRemoteConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        final StringBuilder builderSdp = new StringBuilder();
        builderSdp.append("v=0").append(System.lineSeparator());
        builderSdp.append("o=hrosa 3616 1899 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("s=Talk").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("t=0 0").append(System.lineSeparator());
        builderSdp.append("m=audio 7070 RTP/AVP 8 0 101").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:101 telephone-event/8000");
        builder.append(builderSdp.toString());
        
        final int callId = 1;
        final int transactionId = 147483655;
        final String localSdp = "local-sdp-mock";
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateRemoteConnectionFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpRemoteConnectionImpl primaryConnection = mock(MgcpRemoteConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideRemote(callId)).thenReturn(primaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(1);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).open(eq(builderSdp.toString()), any(FutureCallback.class));
        verify(primaryConnection).updateMode(eq(ConnectionMode.SEND_ONLY), any(FutureCallback.class));
        verify(primaryEndpoint).registerConnection(eq(primaryConnection), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(primaryEndpointId.toString(), result.getParameters().getString(MgcpParameterType.ENDPOINT_ID).get());
        assertEquals(primaryConnection.getHexIdentifier(), result.getParameters().getString(MgcpParameterType.CONNECTION_ID).get());
        assertEquals(localSdp, result.getParameters().getString(MgcpParameterType.SDP).get());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenRegisteringSecondaryLocalConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final int secondaryConnectionId = 7;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);

        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(2, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryEndpoint).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        
        when(primaryEndpoint.isRegistered(callId, primaryConnection.getIdentifier())).thenReturn(true);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(secondaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        when(secondaryEndpoint.isRegistered(callId, secondaryConnection.getIdentifier())).thenReturn(false);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).join(any(MgcpLocalConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).close(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).close(any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);

        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());

        verify(primaryConnection).close(any(FutureCallback.class));
        verify(secondaryConnection).close(any(FutureCallback.class));
        verify(primaryEndpoint).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        verify(secondaryEndpoint, never()).unregisterConnection(eq(callId), eq(secondaryConnectionId), any(FutureCallback.class));

        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenRegisteringPrimaryLocalConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
        
        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final int secondaryConnectionId = 7;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        when(primaryEndpoint.isRegistered(callId, primaryConnection.getIdentifier())).thenReturn(false);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).join(any(MgcpLocalConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).close(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).close(any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).close(any(FutureCallback.class));
        verify(secondaryConnection).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        verify(secondaryEndpoint, never()).unregisterConnection(eq(callId), eq(secondaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenJoiningLocalConnections() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
        
        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final int secondaryConnectionId = 7;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(primaryConnection).join(any(MgcpLocalConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).close(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(secondaryConnection).close(any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).close(any(FutureCallback.class));
        verify(secondaryConnection).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        verify(secondaryEndpoint, never()).unregisterConnection(eq(callId), eq(secondaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenOpeningSecondaryLocalConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
        
        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final int secondaryConnectionId = 7;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess("");
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).close(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));;
                return null;
            }
            
        }).when(secondaryConnection).open(any(String.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).close(any(FutureCallback.class));
        verify(secondaryConnection, never()).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        verify(secondaryEndpoint, never()).unregisterConnection(eq(callId), eq(secondaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenOpeningPrimaryLocalConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateLocalConnectionsFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpLocalConnectionImpl primaryConnection = mock(MgcpLocalConnectionImpl.class);
        final int secondaryConnectionId = 7;
        final MgcpLocalConnectionImpl secondaryConnection = mock(MgcpLocalConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);

        when(connectionProvider.provideLocal(callId)).thenReturn(primaryConnection, secondaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(secondaryConnection.getIdentifier()).thenReturn(secondaryConnectionId);
        when(secondaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(secondaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(secondaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }

        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));

        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);

        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());

        verify(primaryConnection, never()).close(any(FutureCallback.class));
        verify(secondaryConnection, never()).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        verify(secondaryEndpoint, never()).unregisterConnection(eq(callId), eq(secondaryConnectionId), any(FutureCallback.class));

        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenOpeningPrimaryRemoteConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        final StringBuilder builderSdp = new StringBuilder();
        builderSdp.append("v=0").append(System.lineSeparator());
        builderSdp.append("o=hrosa 3616 1899 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("s=Talk").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("t=0 0").append(System.lineSeparator());
        builderSdp.append("m=audio 7070 RTP/AVP 8 0 101").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:101 telephone-event/8000");
        builder.append(builderSdp.toString());
        
        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateRemoteConnectionFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpRemoteConnectionImpl primaryConnection = mock(MgcpRemoteConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideRemote(callId)).thenReturn(primaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection, never()).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenHalfOpeningPrimaryRemoteConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        
        final int callId = 1;
        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateRemoteConnectionFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 4;
        final MgcpRemoteConnectionImpl primaryConnection = mock(MgcpRemoteConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideRemote(callId)).thenReturn(primaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(primaryConnection).halfOpen(any(LocalConnectionOptions.class), any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection, never()).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testRollbackWhenRegisteringPrimaryRemoteConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());
        builder.append(System.lineSeparator());
        final StringBuilder builderSdp = new StringBuilder();
        builderSdp.append("v=0").append(System.lineSeparator());
        builderSdp.append("o=hrosa 3616 1899 IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("s=Talk").append(System.lineSeparator());
        builderSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        builderSdp.append("t=0 0").append(System.lineSeparator());
        builderSdp.append("m=audio 7070 RTP/AVP 8 0 101").append(System.lineSeparator());
        builderSdp.append("a=rtpmap:101 telephone-event/8000");
        builder.append(builderSdp.toString());
        
        final int callId = 1;
        final int transactionId = 147483655;
        final String localSdp = "local-sdp-mock";
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final Parameters<MgcpParameterType> parameters = request.getParameters();
        final CreateConnectionFsmBuilder fsmBuilder = CreateRemoteConnectionFsmBuilder.INSTANCE;
        final CreateConnectionFsm fsm = fsmBuilder.build();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final int primaryConnectionId = 9;
        final MgcpRemoteConnectionImpl primaryConnection = mock(MgcpRemoteConnectionImpl.class);
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionCommand command = new CreateConnectionCommand(context, fsm);
        
        when(connectionProvider.provideRemote(callId)).thenReturn(primaryConnection);
        when(primaryConnection.getIdentifier()).thenReturn(primaryConnectionId);
        when(primaryConnection.getHexIdentifier()).thenCallRealMethod();
        when(primaryConnection.getCallIdentifier()).thenReturn(callId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(primaryEndpoint);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new Exception("test purposes"));
                return null;
            }
            
        }).when(primaryEndpoint).registerConnection(any(MgcpConnection.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<String> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }
            
        }).when(primaryConnection).open(any(String.class), any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(primaryConnection).close(any(FutureCallback.class));
        
        // when
        FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        command.execute(callback);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(captor.capture());
        
        verify(primaryConnection).close(any(FutureCallback.class));
        verify(primaryEndpoint, never()).unregisterConnection(eq(callId), eq(primaryConnectionId), any(FutureCallback.class));
        
        final MgcpCommandResult result = captor.getValue();
        assertEquals(transactionId, result.getTransactionId());
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

}
