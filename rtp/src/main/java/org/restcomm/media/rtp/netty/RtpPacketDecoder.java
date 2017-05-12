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

package org.restcomm.media.rtp.netty;

import java.nio.ByteBuffer;
import java.util.List;

import org.restcomm.media.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * Decodes RTP packets coming from the network.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Sharable
public class RtpPacketDecoder extends MessageToMessageDecoder<DatagramPacket> {

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
        // Get data coming from network
        final ByteBuf readBuffer = msg.content();
        final int offset = readBuffer.arrayOffset();
        final int length = readBuffer.readableBytes();
        final byte[] data = readBuffer.array();

        // Decode data and wrap it
        final RtpPacket rtpPacket = new RtpPacket(length, false);
        final ByteBuffer writeBuffer = rtpPacket.getBuffer();
        writeBuffer.put(data, offset, length).flip();

        // Pass RTP packet down the pipeline
        out.add(rtpPacket);
    }

}
