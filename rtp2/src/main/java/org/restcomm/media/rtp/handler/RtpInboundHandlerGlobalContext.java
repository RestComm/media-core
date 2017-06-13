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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.session.RtpSessionStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 * Context that defines the state of the {@link RtpInboundHandler}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandlerGlobalContext {

    // RTP Components
    private final Clock clock;
    private final RtpSessionStatistics statistics;
    private final JitterBuffer jitterBuffer;
    private final RtpInput rtpInput;
    private final DtmfInput dtmfInput;

    // Handler Context
    private final AtomicReference<RTPFormats> formats;
    private final AtomicBoolean loopable;
    private final AtomicBoolean receivable;

    public RtpInboundHandlerGlobalContext(Clock clock, RtpSessionStatistics statistics, JitterBuffer jitterBuffer, RtpInput rtpInput, DtmfInput dtmfInput) {
        // RTP Components
        this.clock = clock;
        this.statistics = statistics;
        this.rtpInput = rtpInput;
        this.dtmfInput = dtmfInput;
        this.jitterBuffer = jitterBuffer;
        this.jitterBuffer.observe(this.rtpInput);

        // Handler Context
        this.formats = new AtomicReference<RTPFormats>(new RTPFormats());
        this.loopable = new AtomicBoolean(false);
        this.receivable = new AtomicBoolean(false);
    }

    Clock getClock() {
        return clock;
    }

    RtpSessionStatistics getStatistics() {
        return statistics;
    }

    JitterBuffer getJitterBuffer() {
        return jitterBuffer;
    }

    RtpInput getRtpInput() {
        return rtpInput;
    }

    DtmfInput getDtmfInput() {
        return dtmfInput;
    }

    boolean isLoopable() {
        return this.loopable.get();
    }

    void setLoopable(boolean loopable) {
        this.loopable.set(loopable);
    }

    boolean isReceivable() {
        return this.receivable.get();
    }

    void setReceivable(boolean receivable) {
        this.receivable.set(receivable);
    }

    RTPFormats getFormats() {
        return this.formats.get();
    }

    void setFormats(RTPFormats rtpFormats) {
        this.formats.set(rtpFormats);
    }

}
