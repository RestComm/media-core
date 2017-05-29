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
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandlerFsmImpl extends AbstractRtpInboundHandlerFsm {

    private static final Logger log = Logger.getLogger(RtpInboundHandlerFsmImpl.class);

    private final RtpInboundHandlerGlobalContext context;

    public RtpInboundHandlerFsmImpl(RtpInboundHandlerGlobalContext context) {
        super();
        this.context = context;
    }

    @Override
    public void enterActivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
        this.context.getRtpInput().activate();
        this.context.getDtmfInput().activate();
    }

    @Override
    public void enterDeactivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event, RtpInboundHandlerTransactionContext context) {
        this.context.getRtpInput().deactivate();
        this.context.getDtmfInput().deactivate();
        this.context.getDtmfInput().reset();
        this.context.getJitterBuffer().restart();
    }
    
    @Override
    public void onModeChanged(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event, RtpInboundHandlerTransactionContext context) {
        final RtpInboundHandlerUpdateModeContext txContext = (RtpInboundHandlerUpdateModeContext) context;
        final ConnectionMode mode = txContext.getMode();
        
        switch (mode) {
            case INACTIVE:
            case SEND_ONLY:
                this.context.setLoopable(false);
                this.context.setReceivable(false);
                break;

            case RECV_ONLY:
                this.context.setLoopable(false);
                this.context.setReceivable(true);
                break;

            case SEND_RECV:
            case CONFERENCE:
                this.context.setLoopable(false);
                this.context.setReceivable(true);
                break;

            case NETWORK_LOOPBACK:
                this.context.setLoopable(true);
                this.context.setReceivable(false);
                break;

            default:
                this.context.setLoopable(false);
                this.context.setReceivable(false);
                break;
        }
    }
    
    @Override
    public void onFormatChanged(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event,
            RtpInboundHandlerTransactionContext context) {
        final RtpInboundHandlerFormatChangedContext txContext = (RtpInboundHandlerFormatChangedContext) context;
        final RTPFormats formats = txContext.getFormats();
        this.context.setFormats(formats);
    }

    @Override
    public void onPacketReceived(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event, RtpInboundHandlerTransactionContext context) {
        final RtpInboundHandlerPacketReceivedContext txContext = (RtpInboundHandlerPacketReceivedContext) context;
        final RtpPacket packet = txContext.getPacket();
        final int payloadType = packet.getPayloadType();
        final RTPFormat format = this.context.getFormats().find(payloadType);
        final RtpStatistics statistics = this.context.getStatistics();

        // RTP keep-alive
        statistics.setLastHeartbeat(this.context.getClock().getTime());

        // Consume packet
        if (format == null) {
            log.warn("RTP Channel " + statistics.getSsrc() + " dropped packet because payload type " + payloadType
                    + " is unknown.");
        } else {
            if (RtpChannel.DTMF_FORMAT.matches(format.getFormat())) {
                this.context.getDtmfInput().write(packet);
            } else {
                this.context.getJitterBuffer().write(packet, format);
            }
        }
    }

}
