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

import org.restcomm.media.rtp.*;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.RTPFormats;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpSessionImplFactory implements RtpSessionFactory {

    private final AtomicInteger componentIdGenerator;
    private final RtpChannelFactory channelFactory;
    private final RtpInputFactory rtpInputFactory;
    private final RtpOutputFactory rtpOutputFactory;
    private final DtmfInputFactory dtmfInputFactory;
    private final SsrcGenerator ssrcGenerator;
    private final Clock wallClock;
    private final MediaType mediaType;
    private final RTPFormats formats;

    public RtpSessionImplFactory(AtomicInteger componentIdGenerator, RtpChannelFactory channelFactory,
                                 RtpInputFactory rtpInputFactory, RtpOutputFactory rtpOutputFactory, DtmfInputFactory dtmfInputFactory,
                                 SsrcGenerator ssrcGenerator, Clock wallClock, MediaType mediaType, RTPFormats formats) {
        this.componentIdGenerator = componentIdGenerator;
        this.channelFactory = channelFactory;
        this.rtpInputFactory = rtpInputFactory;
        this.rtpOutputFactory = rtpOutputFactory;
        this.dtmfInputFactory = dtmfInputFactory;
        this.ssrcGenerator = ssrcGenerator;
        this.wallClock = wallClock;
        this.mediaType = mediaType;
        this.formats = formats;
    }

    @Override
    public RtpSession build() {
        final long ssrc = this.ssrcGenerator.generateSsrc();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(wallClock, ssrc);
        final RtpClock rtpClock = new RtpClock(this.wallClock);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, this.formats, rtpClock);

        // Build dependencies
        RtpChannel channel = this.channelFactory.build();
        RtpInput rtpInput = this.rtpInputFactory.build();
        RtpOutput rtpOutput = this.rtpOutputFactory.build(context);
        DtmfInput dtmfInput = this.dtmfInputFactory.build();

        // Build RTP Session

        return new RtpSessionImpl(componentIdGenerator.incrementAndGet(), channel, context, rtpInput, dtmfInput, rtpOutput);
    }

}
