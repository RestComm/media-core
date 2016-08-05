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

package org.mobicents.media.control.mgcp.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommandTest {

    @Test
    public void testCreateLocalConnectionsBetweenTwoEndpoints() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpEndpoint ivrEndpoint = mock(MgcpEndpoint.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        when(bridgeEndpoint.createConnection(1, true)).thenReturn(connection1);
        when(ivrEndpoint.createConnection(1, true)).thenReturn(connection2);

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(endpointManager, times(1)).registerEndpoint("mobicents/ivr/");
        verify(bridgeEndpoint, times(1)).createConnection(1, true);
        verify(ivrEndpoint, times(1)).createConnection(1, true);
        verify(connection1, times(1)).join(connection2);
    }

    @Test
    public void testCreateOutboundRemoteConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("L:webrtc:false").append(System.lineSeparator());

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connection.halfOpen(any(LocalConnectionOptions.class))).thenReturn("answer");
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.createConnection(1, false)).thenReturn(connection);

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).createConnection(1, false);
        verify(connection, times(1)).halfOpen(any(LocalConnectionOptions.class));
    }

    @Test
    public void testCreateInboundRemoteConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
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

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(connection.open(builderSdp.toString())).thenReturn("answer");
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.createConnection(1, false)).thenReturn(connection);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals("answer", response.getParameter(MgcpParameterType.SDP));
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message(), response.getMessage());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals("answer", result.getParameters().getString(MgcpParameterType.SDP).or(""));
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).createConnection(1, false);
        verify(connection, times(1)).open(builderSdp.toString());
    }

    @Test
    public void testValidateRequestWithZ2AndSdpPresent() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
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

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithMissingCallId() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithInvalidConnectionMode() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:xywz").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithComplicatedWildcardOnPrimaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/*@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithComplicatedWildcardOnSecondaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/*@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithUnknownPrimaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenThrow(new UnrecognizedMgcpNamespaceException(""));

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), result.getCode());
    }

    @Test
    public void testValidateRequestWithUnknownSecondaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenThrow(new UnrecognizedMgcpNamespaceException(""));

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), result.getCode());
    }

    @Test
    public void testRollback() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final int transactionId = 147483655;
        final MgcpRequest request = new MgcpMessageParser().parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpEndpoint ivrEndpoint = mock(MgcpEndpoint.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection1.getHexIdentifier()).thenReturn(Integer.toHexString(1));
        when(connection2.getIdentifier()).thenReturn(2);
        when(connection2.getHexIdentifier()).thenReturn(Integer.toHexString(1));
        when(bridgeEndpoint.getEndpointId()).thenReturn("mobicents/bridge/1");
        when(ivrEndpoint.getEndpointId()).thenReturn("mobicents/ivr/2");
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        when(endpointManager.getEndpoint("mobicents/bridge/" + bridgeEndpoint.getEndpointId() + "@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(endpointManager.getEndpoint("mobicents/ivr/" + ivrEndpoint.getEndpointId() + "@127.0.0.1:2427")).thenReturn(ivrEndpoint);
        when(bridgeEndpoint.createConnection(1, true)).thenReturn(connection1);
        when(ivrEndpoint.createConnection(1, true)).thenReturn(connection2);

        doThrow(MgcpConnectionException.class).when(connection1).join(connection2);

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
        verify(bridgeEndpoint, times(1)).deleteConnection(1, connection1.getIdentifier());
        verify(ivrEndpoint, times(1)).deleteConnection(1, connection2.getIdentifier());
    }

}
