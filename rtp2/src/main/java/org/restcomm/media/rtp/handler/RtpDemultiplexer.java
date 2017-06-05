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

package org.restcomm.media.rtp.handler;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

/**
 * Handler that filters incoming traffic targeting an RTP Channel and forwards it to the respective decoder.
 * <p>
 * Note that RTP Channels may multiplex RTP, RTCP, STUN and DTLS packets for any given call, depending on setup.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpDemultiplexer extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = Logger.getLogger(RtpDemultiplexer.class);

    public RtpDemultiplexer() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // Differentiate between RTP, STUN and DTLS packets in the pipeline
        // https://tools.ietf.org/html/rfc5764#section-5.1.2
        final int offset = msg.arrayOffset();
        final byte b0 = msg.getByte(offset);
        final int b0Int = b0 & 0xff;

        if (b0Int < 2) {
            // TDODO handleStunPacket(ctx, msg);
        } else if (b0Int > 19 && b0Int < 64) {
            // TODO handleDtlsPacket(ctx, msg);
        } else if (b0Int > 127 && b0Int < 192) {
            handleRtpPacket(ctx, msg);
        } else {
            // Unsupported packet type. Drop it.
            ReferenceCountUtil.release(msg);
            if (log.isDebugEnabled()) {
                log.debug("Channel " + ctx.channel().localAddress() + " dropped unsupported packet type " + b0Int);
            }
        }
    }

    private void handleRtpPacket(ChannelHandlerContext ctx, ByteBuf buffer) {
        RtpPacket rtpPacket = buildRtpPacket(buffer);
        ctx.fireChannelRead(rtpPacket);

        // TODO check cases where RTCP is multiplexed
    }

    private RtpPacket buildRtpPacket(ByteBuf msg) {
        // Retrieve data from network
        final int length = msg.readableBytes();
        final int offset = msg.readerIndex();
        final byte[] data = new byte[length];
        msg.getBytes(offset, data, 0, length);

        // Wrap data into an RTP packet
        return new RtpPacket(data);
    }

}
