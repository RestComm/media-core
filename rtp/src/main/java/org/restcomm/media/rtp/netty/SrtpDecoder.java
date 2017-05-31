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

import java.util.List;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.crypto.PacketTransformer;
import org.restcomm.media.rtp.secure.SrtpPacket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SrtpDecoder extends MessageToMessageDecoder<SrtpPacket> {
    
    private static final Logger log = Logger.getLogger(SrtpDecoder.class);

    private final PacketTransformer decoder;

    public SrtpDecoder(PacketTransformer decoder) {
        super();
        this.decoder = decoder;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, SrtpPacket msg, List<Object> out) throws Exception {
        // Get payload from packet
        int length = msg.getPayloadLength();
        final byte[] data = new byte[length];
        msg.getPayload(data);
        
        // Decode payload
        byte[] decoded = this.decoder.reverseTransform(data, 0, length);
        
        if(decoded == null || decoded.length == 0) {
            // Failed to decode data. Drop packet.
            log.debug("Channel " + ctx.channel().localAddress() + " could not decode incoming SRTP packet.");
        } else {
            // Data decoded successfully
            msg.setPayload(decoded);
            
            // Pass decoded RTP packet to next handler
            out.add((RtpPacket) msg);
        }
    }

}
