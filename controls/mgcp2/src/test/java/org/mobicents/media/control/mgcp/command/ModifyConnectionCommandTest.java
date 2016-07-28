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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionCommandTest {

    @Test
    public void testModifyConnectionMode() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);

        // then
        verify(connection, times(1)).setMode(ConnectionMode.SEND_ONLY);
        verify(connection, never()).open(any(String.class));
    }

    @Test
    public void testModifyRemoteDescription() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
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
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);

        // then
        verify(connection, times(1)).setMode(ConnectionMode.SEND_RECV);
        verify(connection, times(1)).open(builderSdp.toString());
    }

    @Test
    public void testValidateRequestWithEndpointNameContainingWildCardAll() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/*@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

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

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testValidateRequestWithEndpointNameContainingWildCardAny() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

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

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testValidateRequestMissingCallId() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

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

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testValidateRequestMissingConnectionId() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testValidateRequestWithInvalidMode() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:xywz").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

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

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testUnknownEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1")).thenReturn(null);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    public void testUnknownConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append("M:sendonly").append(System.lineSeparator());

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(null);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInvalidRemoteDescription() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("MDCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
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

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final MgcpMessageObserver listener = mock(MgcpMessageObserver.class);
        final ModifyConnectionCommand mdcx = new ModifyConnectionCommand(endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getConnection(1, 1)).thenReturn(connection);
        when(connection.open(builderSdp.toString())).thenThrow(MgcpConnectionException.class);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
                assertNotNull(response);
                assertEquals(MgcpResponseCode.UNSUPPORTED_SDP.code(), response.getCode());
                return null;
            }

        }).when(listener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        mdcx.observe(listener);
        mdcx.execute(request);
    }

}
