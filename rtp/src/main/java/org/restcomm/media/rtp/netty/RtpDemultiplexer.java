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

import org.apache.log4j.Logger;
import org.restcomm.media.rtcp.RtcpHeader;
import org.restcomm.media.rtcp.RtcpPacket;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.secure.DtlsPacket;
import org.restcomm.media.stun.StunException;
import org.restcomm.media.stun.messages.StunMessage;

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

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        // Differentiate between RTP, STUN and DTLS packets in the pipeline
        // https://tools.ietf.org/html/rfc5764#section-5.1.2
        final int offset = msg.arrayOffset();
        final byte b0 = msg.getByte(offset);
        final int b0Int = b0 & 0xff;

        if (b0Int < 2) {
            handleStunPacket(ctx, msg);
        } else if (b0Int > 19 && b0Int < 64) {
            handleDtlsPacket(ctx, msg);
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

    private void handleStunPacket(ChannelHandlerContext ctx, ByteBuf buffer) {
        // Retrieve data from network
        final byte[] data = buffer.array();
        final int length = buffer.readableBytes();
        final int offset = buffer.arrayOffset();

        // Wrap data into an STUN packet
        try {
            StunMessage stunPacket = StunMessage.decode(data, (char) offset, (char) length);
            ctx.fireChannelRead(stunPacket);
        } catch (StunException e) {
            // Drop packet as we could not decode it
            ReferenceCountUtil.release(buffer);
            log.warn("Channel " + ctx.channel().localAddress() + "could not decode incoming STUN packet", e);
        }
    }

    private void handleDtlsPacket(ChannelHandlerContext ctx, ByteBuf buffer) {
        // Retrieve data from network
        final int length = buffer.readableBytes();
        final int offset = buffer.arrayOffset();

        // Wrap data into a DTLS packet
        byte[] data = new byte[length - offset];
        buffer.getBytes(offset, data);
        DtlsPacket dtlsPacket = new DtlsPacket(data);

        ctx.fireChannelRead(dtlsPacket);
    }

    private void handleRtpPacket(ChannelHandlerContext ctx, ByteBuf buffer) {
        // Retrieve data from network
        final int offset = buffer.arrayOffset();
        
        /*
         * When RTP and RTCP packets are multiplexed onto a single port, the RTCP packet type field occupies the same position
         * in the packet as the combination of the RTP marker (M) bit and the RTP payload type (PT). This field can be used to
         * distinguish RTP and RTCP packets when two restrictions are observed:
         * 
         * 1) the RTP payload type values used are distinct from the RTCP packet types used.
         * 
         * 2) for each RTP payload type (PT), PT+128 is distinct from the RTCP packet types used. The first constraint precludes
         * a direct conflict between RTP payload type and RTCP packet type; the second constraint precludes a conflict between
         * an RTP data packet with the marker bit set and an RTCP packet.
         */
        int type = buffer.getByte(offset + 1) & 0xff & 0x7f;
        int rtcpType = type + 128;

        // RTP payload types 72-76 conflict with the RTCP SR, RR, SDES, BYE and APP packets defined in the RTP specification
        switch (rtcpType) {
            case RtcpHeader.RTCP_SR:
            case RtcpHeader.RTCP_RR:
            case RtcpHeader.RTCP_SDES:
            case RtcpHeader.RTCP_BYE:
            case RtcpHeader.RTCP_APP:
                RtcpPacket rtcpPacket = buildRtcpPacket(buffer);
                ctx.fireChannelRead(rtcpPacket);
            default:
                RtpPacket rtpPacket = buildRtpPacket(buffer);
                ctx.fireChannelRead(rtpPacket);
                break;
        }
    }

    private RtpPacket buildRtpPacket(ByteBuf msg) {
        // Retrieve data from network
        final byte[] data = msg.array();
        final int length = msg.readableBytes();
        final int offset = msg.arrayOffset();

        // Wrap data into an RTP packet
        final RtpPacket rtpPacket = new RtpPacket(length - offset, false);
        rtpPacket.getBuffer().put(data, offset, length).flip();
        return rtpPacket;
    }

    private RtcpPacket buildRtcpPacket(ByteBuf msg) {
        // Retrieve data from network
        final byte[] data = msg.array();
        final int offset = msg.arrayOffset();

        // Wrap data into an RTP packet
        final RtcpPacket rtcpPacket = new RtcpPacket();
        rtcpPacket.decode(data, offset);
        return rtcpPacket;
    }

}
