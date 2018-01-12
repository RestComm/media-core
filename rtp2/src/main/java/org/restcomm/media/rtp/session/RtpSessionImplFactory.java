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

import org.restcomm.media.rtp.DtmfInputFactory;
import org.restcomm.media.rtp.JitterBuffer;
import org.restcomm.media.rtp.JitterBufferFactory;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpChannelFactory;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpInputFactory;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.RtpOutputFactory;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.SsrcGenerator;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.RTPFormats;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionImplFactory implements RtpSessionFactory {

    private final RtpChannelFactory channelFactory;
    private final JitterBufferFactory jitterBufferFactory;
    private final RtpInputFactory rtpInputFactory;
    private final RtpOutputFactory rtpOutputFactory;
    private final DtmfInputFactory dtmfInputFactory;
    private final SsrcGenerator ssrcGenerator;
    private final Clock wallClock;
    private final MediaType mediaType;
    private final RTPFormats formats;

    public RtpSessionImplFactory(RtpChannelFactory channelFactory, JitterBufferFactory jitterBufferFactory,
            RtpInputFactory rtpInputFactory, RtpOutputFactory rtpOutputFactory, DtmfInputFactory dtmfInputFactory,
            SsrcGenerator ssrcGenerator, Clock wallClock, MediaType mediaType, RTPFormats formats) {
        this.channelFactory = channelFactory;
        this.jitterBufferFactory = jitterBufferFactory;
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
        // Build dependencies
        RtpChannel channel = this.channelFactory.build();
        JitterBuffer jitterBuffer = this.jitterBufferFactory.build();
        RtpInput rtpInput = this.rtpInputFactory.build();
        RtpOutput rtpOutput = this.rtpOutputFactory.build();
        DtmfInput dtmfInput = this.dtmfInputFactory.build();
        
        // Build RTP Session
        long ssrc = this.ssrcGenerator.generateSsrc();
        RtpSessionStatistics statistics = new RtpSessionStatistics(wallClock, ssrc);
        RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, this.formats);
        RtpSession rtpSession = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);
        return rtpSession;
    }

}
