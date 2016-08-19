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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.EndpointIdentifier;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;

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
        final EndpointIdentifier bridgeEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final EndpointIdentifier ivrEndpointId = new EndpointIdentifier("mobicents/ivr/1", "127.0.0.1:2427");
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection2.getIdentifier()).thenReturn(2);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        when(bridgeEndpoint.getEndpointId()).thenReturn(bridgeEndpointId);
        when(ivrEndpoint.getEndpointId()).thenReturn(ivrEndpointId);
        when(bridgeEndpoint.createConnection(1, true)).thenReturn(connection1);
        when(ivrEndpoint.createConnection(1, true)).thenReturn(connection2);

        MgcpCommandResult result = crcx.call();

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(endpointManager, times(1)).registerEndpoint("mobicents/ivr/");
        verify(bridgeEndpoint, times(1)).createConnection(1, true);
        verify(ivrEndpoint, times(1)).createConnection(1, true);
        verify(connection1, times(1)).join(connection2);
        
        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        
        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(4, result.getParameters().size());
        assertEquals(bridgeEndpoint.getEndpointId().toString(), parameters.getString(MgcpParameterType.ENDPOINT_ID).or(""));
        assertEquals(ivrEndpoint.getEndpointId().toString(), parameters.getString(MgcpParameterType.SECOND_ENDPOINT).or(""));
        assertEquals(connection1.getIdentifier(), parameters.getInteger(MgcpParameterType.CONNECTION_ID).or(0).intValue());
        assertEquals(connection2.getIdentifier(), parameters.getInteger(MgcpParameterType.CONNECTION_ID2).or(0).intValue());
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
        final EndpointIdentifier bridgeEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connection.getIdentifier()).thenReturn(1);
        when(connection.halfOpen(any(LocalConnectionOptions.class))).thenReturn("answer");
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getEndpointId()).thenReturn(bridgeEndpointId);
        when(bridgeEndpoint.createConnection(1, false)).thenReturn(connection);

        MgcpCommandResult result = crcx.call();

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).createConnection(1, false);
        verify(connection, times(1)).halfOpen(any(LocalConnectionOptions.class));

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(3, result.getParameters().size());
        assertEquals(bridgeEndpoint.getEndpointId().toString(), parameters.getString(MgcpParameterType.ENDPOINT_ID).or(""));
        assertEquals(connection.getIdentifier(), parameters.getInteger(MgcpParameterType.CONNECTION_ID).or(0).intValue());
        assertEquals("answer", parameters.getString(MgcpParameterType.SDP).or(""));
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
        final EndpointIdentifier bridgeEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideRemote()).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(1);
        when(connection.open(builderSdp.toString())).thenReturn("answer");
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getEndpointId()).thenReturn(bridgeEndpointId);
        when(bridgeEndpoint.createConnection(1, false)).thenReturn(connection);

        MgcpCommandResult result = crcx.call();

        // then
        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
        verify(bridgeEndpoint, times(1)).createConnection(1, false);
        verify(connection, times(1)).open(builderSdp.toString());

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals("answer", result.getParameters().getString(MgcpParameterType.SDP).or(""));

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(3, result.getParameters().size());
        assertEquals(bridgeEndpoint.getEndpointId().toString(), parameters.getString(MgcpParameterType.ENDPOINT_ID).or(""));
        assertEquals(connection.getIdentifier(), parameters.getInteger(MgcpParameterType.CONNECTION_ID).or(0).intValue());
        assertEquals("answer", parameters.getString(MgcpParameterType.SDP).or(""));
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
        assertEquals(0, result.getParameters().size());
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
        assertEquals(0, result.getParameters().size());
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
        assertEquals(0, result.getParameters().size());
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
        assertEquals(0, result.getParameters().size());
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
        assertEquals(0, result.getParameters().size());
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
        assertEquals(0, result.getParameters().size());
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
        final EndpointIdentifier bridgeEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenThrow(new UnrecognizedMgcpNamespaceException(""));
        when(bridgeEndpoint.getEndpointId()).thenReturn(bridgeEndpointId);

        MgcpCommandResult result = crcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE.code(), result.getCode());
        assertEquals(0, result.getParameters().size());
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
        final EndpointIdentifier bridgeEndpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        final EndpointIdentifier ivrEndpointId = new EndpointIdentifier("mobicents/ivr/2", "127.0.0.1:2427");
        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final CreateConnectionCommand crcx = new CreateConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(connectionProvider.provideLocal()).thenReturn(connection1, connection2);
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection1.getHexIdentifier()).thenReturn(Integer.toHexString(1));
        when(connection2.getIdentifier()).thenReturn(2);
        when(connection2.getHexIdentifier()).thenReturn(Integer.toHexString(1));
        when(bridgeEndpoint.getEndpointId()).thenReturn(bridgeEndpointId);
        when(ivrEndpoint.getEndpointId()).thenReturn(ivrEndpointId);
        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
        when(endpointManager.getEndpoint(bridgeEndpoint.getEndpointId().toString())).thenReturn(bridgeEndpoint);
        when(endpointManager.getEndpoint(ivrEndpoint.getEndpointId().toString())).thenReturn(ivrEndpoint);
        when(bridgeEndpoint.createConnection(1, true)).thenReturn(connection1);
        when(ivrEndpoint.createConnection(1, true)).thenReturn(connection2);

        doThrow(MgcpConnectionException.class).when(connection1).join(connection2);

        MgcpCommandResult result = crcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnection(1, connection1.getIdentifier());
        verify(ivrEndpoint, times(1)).deleteConnection(1, connection2.getIdentifier());

        assertNotNull(result);
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
        assertEquals(0, result.getParameters().size());
    }

}
