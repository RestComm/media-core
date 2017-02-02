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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.util.collections.Parameters;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionCommandTest {

    @Test
    public void testDeleteSingleConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnection(1, 1)).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(1);

        MgcpCommandResult result = dlcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnection(1, 1);

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertNotNull(result.getParameters().getString(MgcpParameterType.CONNECTION_PARAMETERS).orNull());
        
        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertTrue(parameters.getString(MgcpParameterType.CONNECTION_PARAMETERS).isPresent());
        assertEquals(1, parameters.size());
    }

    @Test
    public void testDeleteAllCallConnections() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpRemoteConnection connection1 = mock(MgcpRemoteConnection.class);
        final MgcpLocalConnection connection2 = mock(MgcpLocalConnection.class);
        final List<MgcpConnection> connections = new ArrayList<>();
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnections(1)).thenReturn(connections);
        connections.add(connection1);
        connections.add(connection2);

        MgcpCommandResult result = dlcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnections(1);

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertNull(result.getParameters().getString(MgcpParameterType.CONNECTION_PARAMETERS).orNull());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testDeleteAllEndpointConnections() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final List<MgcpConnection> connections = new ArrayList<>();
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnections()).thenReturn(connections);
        MgcpCommandResult result = dlcx.call();
        
        // then
        verify(bridgeEndpoint, times(1)).deleteConnections();

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertNull(result.getParameters().getString(MgcpParameterType.CONNECTION_PARAMETERS).orNull());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testNoConnectionFoundWhenDeletingSingleConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnection(1, 1)).thenThrow(new MgcpConnectionNotFoundException(""));

        MgcpCommandResult result = dlcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnection(1, 1);

        assertNotNull(result);
        assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());        
    }

    @Test
    public void testNoCallFoundWhenDeletingSingleConnection() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnection(1, 1)).thenThrow(new MgcpCallNotFoundException(""));

        MgcpCommandResult result = dlcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnection(1, 1);

        assertNotNull(result);
        assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testNoCallFoundWhenDeletingMultipleConnections() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.deleteConnections(1)).thenThrow(new MgcpCallNotFoundException(""));

        MgcpCommandResult result = dlcx.call();

        // then
        verify(bridgeEndpoint, times(1)).deleteConnections(1);

        assertNotNull(result);
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertNull(result.getParameters().getString(MgcpParameterType.CONNECTION_PARAMETERS).orNull());
    }

    @Test
    public void testValidateRequestWithEndpointNameContainingWildCardAllWithEndpointIdSpecified() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/*@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = dlcx.call();
        
        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testValidateRequestWithEndpointNameContainingWildCardAnyWithEndpointIdSpecified() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("I:1").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = dlcx.call();
        
        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testValidateRequestWithEndpointNameContainingWildCardAnyWithoutEndpointIdSpecified() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        MgcpCommandResult result = dlcx.call();
        
        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testUnknownEndpoint() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenReturn(null);

        MgcpCommandResult result = dlcx.call();
        
        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }

    @Test
    public void testUnexpectedExceptionWhileExecutingCommand() throws MgcpException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("DLCX 147483653 mobicents/bridge/1@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        final int transactionId = 147483653;
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionCommand dlcx = new DeleteConnectionCommand(transactionId, request.getParameters(), endpointManager);

        // when
        when(endpointManager.getEndpoint("mobicents/bridge/1@127.0.0.1:2427")).thenThrow(new RuntimeException("Test Purposes!"));

        MgcpCommandResult result = dlcx.call();

        // then
        assertNotNull(result);
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());

        Parameters<MgcpParameterType> parameters = result.getParameters();
        assertEquals(0, parameters.size());
    }
}
