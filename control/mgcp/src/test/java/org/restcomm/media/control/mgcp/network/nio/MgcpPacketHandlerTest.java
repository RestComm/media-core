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

package org.restcomm.media.control.mgcp.network.nio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.network.nio.MgcpPacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpPacketHandlerTest {

    @Test
    public void testHandleIncomingRequest() throws PacketHandlerException, MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427");
        final byte[] data = builder.toString().getBytes();
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpMessageParser parser = mock(MgcpMessageParser.class);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpPacketHandler handler = new MgcpPacketHandler(parser);

        // when
        when(parser.parseRequest(data, 0, data.length)).thenReturn(request);
        handler.observe(observer);
        handler.handle(data, to, from);

        // then
        verify(parser, times(1)).parseRequest(data, 0, data.length);
        verify(observer, times(1)).onMessage(from, to, request, MessageDirection.INCOMING);
    }

    @Test
    public void testHandleIncomingResponse() throws PacketHandlerException, MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("200 147483653 Successful Transaction").append(System.lineSeparator());
        builder.append("I:1f").append(System.lineSeparator());
        builder.append("Z:mobicents/bridge/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/1@127.0.0.1:2427").append(System.lineSeparator());
        builder.append("I2:10").append(System.lineSeparator());
        final byte[] data = builder.toString().getBytes();
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpMessageParser parser = mock(MgcpMessageParser.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpPacketHandler handler = new MgcpPacketHandler(parser);

        // when
        when(parser.parseResponse(data, 0, data.length)).thenReturn(response);
        handler.observe(observer);
        handler.handle(data, to, from);

        // then
        verify(parser, times(1)).parseResponse(data, 0, data.length);
        verify(observer, times(1)).onMessage(from, to, response, MessageDirection.INCOMING);
    }

}
