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
import org.restcomm.media.rtp.RtpStatistics;
import org.restcomm.media.rtp.format.DtmfFormat;
import org.restcomm.media.sdp.format.RTPFormat;

/**
 * Handles incoming RTP traffic.
 * 
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
    public void enterActivated(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event, RtpInboundHandlerTransactionContext context) {
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
    public void onPacketReceived(RtpInboundHandlerState from, RtpInboundHandlerState to, RtpInboundHandlerEvent event, RtpInboundHandlerTransactionContext context) {
        final RtpInboundHandlerPacketReceivedContext txContext = (RtpInboundHandlerPacketReceivedContext) context;
        final RtpPacket packet = txContext.getPacket();
        final int payloadType = packet.getPayloadType();
        final RTPFormat format = this.context.getFormats().find(payloadType);
        final RtpStatistics statistics = this.context.getStatistics();

        if (format == null) {
            // Drop packet with unknown format
            log.warn("RTP Channel " + statistics.getSsrc() + " dropped packet because payload type " + payloadType + " is unknown.");
        } else {
            // Consume packet
            if (DtmfFormat.FORMAT.matches(format.getFormat())) {
                this.context.getDtmfInput().write(packet);
            } else {
                this.context.getJitterBuffer().write(packet, format);
            }

            // Update statistics
            statistics.incomingRtp(packet);
        }
    }

}
