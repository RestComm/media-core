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

package org.restcomm.media.bootstrap.ioc.provider.rtp;

import java.util.Iterator;

import org.restcomm.media.core.configuration.CodecType;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.rtp.DtmfInputFactory;
import org.restcomm.media.rtp.JitterBufferFactory;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpChannelFactory;
import org.restcomm.media.rtp.RtpInputFactory;
import org.restcomm.media.rtp.RtpOutputFactory;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.SsrcGenerator;
import org.restcomm.media.rtp.session.RtpSessionImplFactory;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionFactoryGuiceProvider implements Provider<RtpSessionFactory> {

    private final MediaServerConfiguration configuration;
    private final SsrcGenerator ssrcGenerator;
    private final DtmfInputFactory dtmfInputFactory;
    private final RtpInputFactory rtpInputFactory;
    private final RtpOutputFactory rtpOutputFactory;
    private final RtpChannelFactory channelFactory;
    private final JitterBufferFactory jitterBufferFactory;
    private final Clock wallClock;
    private final MediaType mediaType = MediaType.AUDIO;

    @Inject
    public RtpSessionFactoryGuiceProvider(MediaServerConfiguration configuration, SsrcGenerator ssrcGenerator,
            RtpInputFactory rtpInputFactory, RtpOutputFactory rtpOutputFactory, RtpChannelFactory rtpChannelFactory,
            DtmfInputFactory dtmfInputFactory, JitterBufferFactory jitterBufferFactory, Clock wallClock) {
        super();
        this.configuration = configuration;
        this.ssrcGenerator = ssrcGenerator;
        this.rtpInputFactory = rtpInputFactory;
        this.rtpOutputFactory = rtpOutputFactory;
        this.dtmfInputFactory = dtmfInputFactory;
        this.channelFactory = rtpChannelFactory;
        this.jitterBufferFactory = jitterBufferFactory;
        this.wallClock = wallClock;
    }

    @Override
    public RtpSessionFactory get() {
        RTPFormats formats = provideFormats();
        return new RtpSessionImplFactory(channelFactory, jitterBufferFactory, rtpInputFactory, rtpOutputFactory, dtmfInputFactory, ssrcGenerator, wallClock, mediaType, formats);
    }

    private RTPFormats provideFormats() {
        Iterator<String> codecs = this.configuration.getMediaConfiguration().getCodecs();
        int codecCount = this.configuration.getMediaConfiguration().countCodecs();
        RTPFormats formats = new RTPFormats(codecCount);

        while (codecs.hasNext()) {
            String codecName = codecs.next();
            CodecType codec = CodecType.fromName(codecName);
            if (codec != null) {
                RTPFormat format = AVProfile.audio.find(codec.getPayloadType());
                if (format != null) {
                    formats.add(format.clone());
                }
            }
        }
        return formats;
    }

}
