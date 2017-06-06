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

package org.restcomm.media.rtp;

import org.restcomm.media.scheduler.Clock;

/**
 * Exposes statistics of an RTP session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpStatistics {

    // Core Components
    private final Clock wallClock;

    // Media Session Statistics
    private final long ssrc;

    private int rtpRxPackets;
    private int rtpRxOctets;
    private long rtpReceivedOn;

    private int rtpTxPackets;
    private int rtpTxOctets;
    private long rtpSentOn;

    public RtpStatistics(Clock wallClock, long ssrc) {
        super();

        // Core Components
        this.wallClock = wallClock;

        // Media Session Statistics
        this.ssrc = ssrc;

        this.rtpRxPackets = 0;
        this.rtpRxOctets = 0;
        this.rtpReceivedOn = 0;

        this.rtpTxPackets = 0;
        this.rtpTxOctets = 0;
        this.rtpSentOn = 0;
    }

    public void incomingRtp(RtpPacket packet) {
        this.rtpRxPackets++;
        this.rtpRxOctets += packet.getPayloadLength();
        this.rtpReceivedOn = this.wallClock.getTime();
    }

    public void outgoingRtp(RtpPacket packet) {
        this.rtpTxPackets++;
        this.rtpTxOctets += packet.getPayloadLength();
        this.rtpSentOn = this.wallClock.getTime();
    }

    public long getSsrc() {
        return ssrc;
    }

    public int getRtpPacketsReceived() {
        return rtpRxPackets;
    }

    public int getRtpOctetsReceived() {
        return rtpRxOctets;
    }

    public int getRtpPacketsSent() {
        return rtpTxPackets;
    }

    public int getRtpOctetsSent() {
        return rtpTxOctets;
    }

}
