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

package org.mobicents.media.control.mgcp.endpoint.command;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.CreateConnectionCommand;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.control.mgcp.listener.MgcpCommandListener;
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
public class CreateConnectionCommandTest {

    @Test
    public void testCreateLocalConnection() throws MgcpException {
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

//        // when
//        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
//        when(endpointManager.registerEndpoint("mobicents/ivr/")).thenReturn(ivrEndpoint);
//        when(bridgeEndpoint.createConnection(1, ConnectionMode.SEND_ONLY, ivrEndpoint)).thenReturn(connection1);
//        when(ivrEndpoint.createConnection(1, ConnectionMode.SEND_RECV, bridgeEndpoint)).thenReturn(connection2);
//        doAnswer(new Answer<Object>() {
//
//            @Override
//            public Object answer(InvocationOnMock invocation) throws Throwable {
//                MgcpResponse response = invocation.getArgumentAt(0, MgcpResponse.class);
//                assertNotNull(response);
//                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), response.getCode());
//                assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message(), response.getMessage());
//                return null;
//            }
//        }).when(listener).onCommandExecuted(any(MgcpResponse.class));
//        crcx.execute(request, listener);
//
//        // then
//        verify(endpointManager, times(1)).registerEndpoint("mobicents/bridge/");
//        verify(endpointManager, times(1)).registerEndpoint("mobicents/ivr/");
//        verify(bridgeEndpoint, times(1)).createConnection(1, ConnectionMode.SEND_ONLY, ivrEndpoint);
//        verify(ivrEndpoint, times(1)).createConnection(1, ConnectionMode.SEND_RECV, bridgeEndpoint);
//        verify(connection1, times(1)).join(connection2);
    }

//    @Test
//    public void testCreateOutboundRemoteConnection() throws MgcpException {
//        // given
//        StringBuilder builder = new StringBuilder();
//        builder.append("CRCX 147483655 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
//        builder.append("C:1").append(System.lineSeparator());
//        builder.append("M:sendrecv").append(System.lineSeparator());
//        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
//        builder.append("L:webrtc:false").append(System.lineSeparator());
//        
//        final MgcpMessageParser parser = new MgcpMessageParser();
//        final MgcpRequest request = parser.parseRequest(builder.toString());
//        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
//        final MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
//        final MgcpLocalConnection connection1 = mock(MgcpLocalConnection.class);
//        final MgcpCommandListener listener = mock(MgcpCommandListener.class);
//        final CreateConnectionCommand crcx = new CreateConnectionCommand(endpointManager);
//        
//        // when
//        when(endpointManager.registerEndpoint("mobicents/bridge/")).thenReturn(bridgeEndpoint);
//        when(bridgeEndpoint.cre)
//        crcx.execute(request, listener);
//        
//        // then
//    }

}
