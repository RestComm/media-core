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

package org.restcomm.media.control.mgcp.command.mdcx;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionCommandTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testModifyConnectionMode() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 restcomm/mock/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection = mock(MgcpConnection.class);

        final ModifyConnectionFsm fsm = ModifyConnectionFsmBuilder.INSTANCE.build();
        final ModifyConnectionContext context = new ModifyConnectionContext(request.getTransactionId(), request.getParameters(), endpointManager);

        when(endpointManager.getEndpoint("restcomm/mock/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);

        // when
        final ModifyConnectionCommand command = new ModifyConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);

        command.execute(callback);

        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> callbackCaptor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(ConnectionMode.SEND_ONLY), callbackCaptor.capture());

        // when
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        callbackCaptor.getValue().onSuccess(null);
        verify(callback, timeout(100)).onSuccess(resultCaptor.capture());

        // then
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(0, result.getParameters().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModifyConnectionModeFailure() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 restcomm/mock/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection = mock(MgcpConnection.class);
        
        final ModifyConnectionFsm fsm = ModifyConnectionFsmBuilder.INSTANCE.build();
        final ModifyConnectionContext context = new ModifyConnectionContext(request.getTransactionId(), request.getParameters(), endpointManager);
        
        when(endpointManager.getEndpoint("restcomm/mock/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        
        // when
        final ModifyConnectionCommand command = new ModifyConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        command.execute(callback);
        
        // then
        final ArgumentCaptor<UpdateConnectionModeCallback> callbackCaptor = ArgumentCaptor.forClass(UpdateConnectionModeCallback.class);
        verify(connection).updateMode(eq(ConnectionMode.SEND_ONLY), callbackCaptor.capture());
        
        // when
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        final Exception error = new Exception("test purposes");
        callbackCaptor.getValue().onFailure(error);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        
        // then
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
        assertEquals(0, result.getParameters().size());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testNegotiateConnection() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 restcomm/mock/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
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

        final String localSdp = "local-sdp-mock";
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection = mock(MgcpConnection.class);

        final ModifyConnectionFsm fsm = ModifyConnectionFsmBuilder.INSTANCE.build();
        final ModifyConnectionContext context = new ModifyConnectionContext(request.getTransactionId(), request.getParameters(), endpointManager);

        when(endpointManager.getEndpoint("restcomm/mock/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                NegotiateConnectionCallback callback = invocation.getArgumentAt(1, NegotiateConnectionCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }
            
        }).when(connection).negotiate(any(String.class), any(NegotiateConnectionCallback.class));

        // when
        final ModifyConnectionCommand command = new ModifyConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);

        command.execute(callback);

        // then
        verify(connection, never()).updateMode(any(ConnectionMode.class), any(UpdateConnectionModeCallback.class));
        verify(connection).negotiate(eq(request.getParameter(MgcpParameterType.SDP)), any(NegotiateConnectionCallback.class));

        // when
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());

        // then
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(localSdp, result.getParameters().getString(MgcpParameterType.SDP).or(""));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNegotiateAndUpdateConnectionMode() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 restcomm/mock/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
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
        
        final String localSdp = "local-sdp-mock";
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpConnection connection = mock(MgcpConnection.class);
        
        final ModifyConnectionFsm fsm = ModifyConnectionFsmBuilder.INSTANCE.build();
        final ModifyConnectionContext context = new ModifyConnectionContext(request.getTransactionId(), request.getParameters(), endpointManager);
        
        when(endpointManager.getEndpoint("restcomm/mock/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                NegotiateConnectionCallback callback = invocation.getArgumentAt(1, NegotiateConnectionCallback.class);
                callback.onSuccess(localSdp);
                return null;
            }
            
        }).when(connection).negotiate(any(String.class), any(NegotiateConnectionCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                UpdateConnectionModeCallback callback = invocation.getArgumentAt(1, UpdateConnectionModeCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(connection).updateMode(any(ConnectionMode.class), any(UpdateConnectionModeCallback.class));
        
        // when
        final ModifyConnectionCommand command = new ModifyConnectionCommand(context, fsm);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        command.execute(callback);
        
        // then
        verify(connection, timeout(50)).updateMode(eq(ConnectionMode.SEND_ONLY), any(UpdateConnectionModeCallback.class));
        verify(connection, timeout(50)).negotiate(eq(request.getParameter(MgcpParameterType.SDP)), any(NegotiateConnectionCallback.class));
        
        // when
        final ArgumentCaptor<MgcpCommandResult> resultCaptor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback, timeout(50)).onSuccess(resultCaptor.capture());
        
        // then
        final MgcpCommandResult result = resultCaptor.getValue();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(localSdp, result.getParameters().getString(MgcpParameterType.SDP).or(""));
    }
    
}
