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

package org.restcomm.media.rtp.session;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpPacket;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionOutgoingRtpCallback implements FutureCallback<Void> {
    
    private final Logger log = Logger.getLogger(RtpSessionOutgoingRtpCallback.class);

    private final long ssrc;
    private final RtpSessionStatistics statistics;
    private final RtpPacket packet;

    public RtpSessionOutgoingRtpCallback(long ssrc, RtpSessionStatistics statistics, RtpPacket packet) {
        this.ssrc = ssrc;
        this.statistics = statistics;
        this.packet = packet;
    }

    @Override
    public void onSuccess(Void result) {
        // Update statistics
        this.statistics.outgoingRtp(this.packet);
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("RTP session " + this.ssrc + " could not send an RTP packet to remote peer.", t);
    }

}
