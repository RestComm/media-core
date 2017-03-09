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

package org.restcomm.media.control.mgcp.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageEncoderTest {

    @Test
    public void testEncode() throws Exception {
        // given
        final StringBuilder builder = new StringBuilder();
        builder.append("CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0").append(System.lineSeparator());
        builder.append("C:1").append(System.lineSeparator());
        builder.append("M:sendrecv").append(System.lineSeparator());
        builder.append("N:restcomm@127.0.0.1:2727").append(System.lineSeparator());
        builder.append("Z2:mobicents/ivr/$@127.0.0.1:2427").append(System.lineSeparator());

        final InetSocketAddress sender = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress recipient = new InetSocketAddress("127.0.0.1", 2427);
        final String message = builder.toString();
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpMessage mgcpMessage = parser.parseRequest(message);
        final ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        final List<Object> out = new ArrayList<>(1);
        final MgcpMessageEncoder encoder = new MgcpMessageEncoder();

        // when
        mgcpMessage.setRecipient(recipient);
        mgcpMessage.setSender(sender);
        encoder.encode(context, mgcpMessage, out);

        // then
        assertFalse(out.isEmpty());
        assertTrue(out.get(0) instanceof DatagramPacket);
        
        DatagramPacket packet = (DatagramPacket) out.get(0);
        assertEquals(recipient, packet.recipient());
        assertEquals(sender, packet.sender());
        
    }

}
