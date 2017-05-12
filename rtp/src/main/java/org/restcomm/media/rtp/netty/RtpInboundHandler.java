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
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;

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

    // RTP Components
    private final RtpClock rtpClock;
    private final RtpStatistics statistics;
    private final JitterBuffer jitterBuffer;
    private final RTPInput rtpInput;
    private final DtmfInput dtmfInput;

    // Handler Context
    private RTPFormats rtpFormats;
    private final AtomicBoolean loopable;
    private final AtomicBoolean receivable;

    public RtpInboundHandler(RtpClock rtpClock, RtpStatistics statistics, JitterBuffer jitterBuffer, RTPInput rtpInput, DtmfInput dtmfInput) {
        super();

        // RTP Components
        this.rtpClock = rtpClock;
        this.statistics = statistics;
        this.rtpInput = rtpInput;
        this.dtmfInput = dtmfInput;
        this.jitterBuffer = jitterBuffer;
        this.jitterBuffer.setListener(this.rtpInput);

        // Handler Context
        this.rtpFormats = new RTPFormats();
        this.loopable = new AtomicBoolean(false);
        this.receivable = new AtomicBoolean(false);
    }
    
    public boolean isLoopable() {
        return this.loopable.get();
    }
    
    public void setLoopable(boolean loopable) {
        this.loopable.set(loopable);
    }
    
    public boolean isReceivable() {
        return this.receivable.get();
    }
    
    public void setReceivable(boolean receivable) {
        this.receivable.set(receivable);
    }
    
    public void useJitterBuffer(boolean use) {
        this.jitterBuffer.setInUse(use);
    }
    
    public RTPInput getRtpInput() {
        return rtpInput;
    }
    
    public DtmfInput getDtmfInput() {
        return dtmfInput;
    }
    
    public RTPFormats getRtpFormats() {
        return rtpFormats;
    }
    
    public void setRtpFormats(RTPFormats rtpFormats) {
        this.rtpFormats = rtpFormats;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RtpPacket msg) throws Exception {
        // For RTP keep-alive purposes
        this.statistics.setLastHeartbeat(this.rtpClock.getWallClock().getTime());

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
                log.debug("RTP Channel " + this.statistics.getSsrc() + " dropped packet because it was empty.");
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
