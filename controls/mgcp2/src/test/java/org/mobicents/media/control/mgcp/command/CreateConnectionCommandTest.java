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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.CreateConnectionCommand;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.listener.MgcpCommandListener;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
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
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpEndpoint ivrEndpoint = mock(MgcpEndpoint.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message(), response.getMessage());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(endpointManager, times(1)).registerEndpoint("mobicents/ivr/");
        verify(bridgeEndpoint, times(1)).addConnection(1, connection1);
        verify(ivrEndpoint, times(1)).addConnection(1, connection2);
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

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(connection.halfOpen(any(LocalConnectionOptions.class))).thenReturn("answer");
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
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

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).addConnection(1, connection);
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

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(connection.open(builderSdp.toString())).thenReturn("answer");
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
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

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).addConnection(1, connection);
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

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithMissingCallId() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithInvalidConnectionMode() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:xywz").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithComplicatedWildcardOnPrimaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/*@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithComplicatedWildcardOnSecondaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/*@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithUnknownPrimaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenThrow(new UnrecognizedMgcpNamespaceException(""));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testValidateRequestWithUnknownSecondaryEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenThrow(new UnrecognizedMgcpNamespaceException(""));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        crcx.execute(request, listener);
    }

    @Test
    public void testRollback() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpEndpoint ivrEndpoint = mock(MgcpEndpoint.class);
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager, connectionProvider);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection2.getIdentifier()).thenReturn(2);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), response.getCode());
                return null;
            }

        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
        doThrow(MgcpConnectionException.class).when(connection1).join(connection2);
        crcx.execute(request, listener);

        // then
        verify(bridgeEndpoint, times(1)).deleteConnection(1, connection1.getIdentifier());
        verify(ivrEndpoint, times(1)).deleteConnection(1, connection2.getIdentifier());
    }

}
