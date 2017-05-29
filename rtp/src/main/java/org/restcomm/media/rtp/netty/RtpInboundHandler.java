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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RTPInput;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Handler that processes incoming RTP packets for audio or RFC2833 DTMF.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandler extends SimpleChannelInboundHandler<RtpPacket> {

    private static final Logger log = Logger.getLogger(RtpInboundHandler.class);

    private final RtpInboundHandlerGlobalContext context;
    private final RtpInboundHandlerFsm fsm;

    public RtpInboundHandler(RtpInboundHandlerGlobalContext context) {
        this.context = context;
        this.fsm = RtpInboundHandlerFsmBuilder.INSTANCE.build(context);
        this.fsm.start();
    }

    public void activate() {
        this.fsm.fire(RtpInboundHandlerEvent.ACTIVATE);
    }

    public void deactivate() {
        this.fsm.fire(RtpInboundHandlerEvent.DEACTIVATE);
    }

    public void updateMode(ConnectionMode mode) {
        RtpInboundHandlerUpdateModeContext txContext = new RtpInboundHandlerUpdateModeContext(mode);
        this.fsm.fire(RtpInboundHandlerEvent.MODE_CHANGED, txContext);
    }

    public void useJitterBuffer(boolean use) {
        this.context.getJitterBuffer().setInUse(use);
    }

    public RTPInput getRtpInput() {
        return context.getRtpInput();
    }

    public DtmfInput getDtmfInput() {
        return context.getDtmfInput();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtpPacket msg) throws Exception {
        // Process incoming packet
        RtpInboundHandlerPacketReceivedContext txContext = new RtpInboundHandlerPacketReceivedContext(msg);
        this.fsm.fire(RtpInboundHandlerEvent.PACKET_RECEIVED, txContext);
        
        // Send packet back if channel is operation in NETWORK_LOOPBACK mode
        if(context.isLoopable()) {
            ctx.channel().writeAndFlush(msg);
        }
        

        /////////////////////////////////////////////////////////////////////////////////////////
        // For RTP keep-alive purposes
        this.statistics.setLastHeartbeat(this.clock.getTime());

        // RTP v0 packets are used in some applications. Discarded since we do not handle them.
        int version = msg.getVersion();
        if (version == 0) {
            if (log.isDebugEnabled()) {
                log.debug("RTP Channel " + this.statistics.getSsrc() + " dropped RTP v0 packet.");
            }
            return;
        }

        // Check if channel can receive traffic
        boolean canReceive = (this.receivable.get() || this.loopable.get());
        if (!canReceive) {
            if (log.isDebugEnabled()) {
                log.debug("RTP Channel " + this.statistics.getSsrc()
                        + " dropped packet because channel mode does not allow to receive traffic.");
            }
            return;
        }

        // Check if packet is not empty
        boolean hasData = (msg.getBuffer().limit() > 0);
        if (!hasData) {
            if (log.isDebugEnabled()) {
                log.debug("RTP Channel " + this.statistics.getSsrc() + " dropped packet because payload was empty.");
            }
            return;
        }

        // Process incoming packet
        if (this.loopable.get()) {
            // Update channel statistics
            this.statistics.onRtpReceive(msg);
            this.statistics.onRtpSent(msg);

            // Send back same packet (looping)
            ctx.channel().writeAndFlush(msg);
        } else {
            // Update channel statistics
            this.statistics.onRtpReceive(msg);

            // Write packet
            int payloadType = msg.getPayloadType();
            RTPFormat format = this.rtpFormats.find(payloadType);
            if (format != null) {
                if (RtpChannel.DTMF_FORMAT.matches(format.getFormat())) {
                    this.dtmfInput.write(msg);
                } else {
                    this.jitterBuffer.write(msg, format);
                }
            } else {
                log.warn("RTP Channel " + this.statistics.getSsrc() + " dropped packet because payload type " + payloadType
                        + " is unknown.");
            }
        }
    }

}
