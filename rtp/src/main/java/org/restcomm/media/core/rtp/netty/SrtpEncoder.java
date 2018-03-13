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

package org.restcomm.media.core.rtp.netty;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.rtp.RtpPacket;
import org.restcomm.media.core.rtp.crypto.PacketTransformer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SrtpEncoder extends MessageToByteEncoder<RtpPacket> {

    private static final Logger log = LogManager.getLogger(SrtpEncoder.class);

    private final PacketTransformer encoder;

    public SrtpEncoder(PacketTransformer encoder) {
        super();
        this.encoder = encoder;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, RtpPacket msg, ByteBuf out) throws Exception {
        // Encode RTP packet
        final byte[] rawData = msg.getRawData();
        final byte[] encoded = this.encoder.transform(rawData);

        if (encoded == null || encoded.length == 0) {
            // Failed to encode packet. Drop it.
            log.warn("Channel " + ctx.channel().localAddress() + " could not encode outgoing SRTP packet.");
        } else {
            // Send encoded packet over the wire
            out.writeBytes(encoded);
        }
    }

}
