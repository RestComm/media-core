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

import org.restcomm.media.core.rtp.RtpPacket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpPacketFilter extends SimpleChannelInboundHandler<RtpPacket> {

    public RtpPacketFilter() {
        super(false);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtpPacket msg) throws Exception {
        // Drop message is packet does not contain any payload
        int payloadLength = msg.getPayloadLength();
        if (payloadLength <= 0) {
            ReferenceCountUtil.release(msg);
            return;
        }

        // Drop message if RTP version is unsupported
        int version = msg.getVersion();
        if (RtpPacket.VERSION != version) {
            ReferenceCountUtil.release(msg);
            return;
        }

    }

}
