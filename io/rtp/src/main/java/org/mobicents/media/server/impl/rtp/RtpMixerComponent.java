/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.MixerComponent;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSink;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMixerComponent extends MixerComponent implements RtpGateway {

    private static final Logger logger = Logger.getLogger(RtpMixerComponent.class);

    private final static int DEFAULT_BUFFER_SIZER = 50;

    // Media source
    private final RtpSource rtpSource;
    private final DtmfSource dtmfSource;
    private final JitterBuffer jitterBuffer;

    // Media sink
    private final RtpSink rtpSink;
    private final DtmfSink dtmfSink;

    // RTP transport
    private final RtpTransport rtpTransport;

    // RTP statistics
    private volatile int rxPackets;

    public RtpMixerComponent(int connectionId, Scheduler scheduler, DspFactory dspFactory, RtpTransport channel,
            RtpClock rtpClock, RtpClock oobClock) {
        super(connectionId);

        // RTP source
        this.jitterBuffer = new JitterBuffer(rtpClock, DEFAULT_BUFFER_SIZER);
        try {
            this.rtpSource = new RtpSource(scheduler, jitterBuffer, dspFactory.newProcessor());
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            // exception may happen only if invalid classes have been set in
            // the media server configuration.
            throw new RuntimeException("There are invalid classes specified in the configuration.", e);
        }
        this.dtmfSource = new DtmfSource(scheduler, oobClock);

        // RTP sink
        try {
            this.rtpSink = new RtpSink(scheduler, rtpClock, dspFactory.newProcessor(), this);
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            // exception may happen only if invalid classes have been set in
            // the media server configuration.
            throw new RuntimeException("There are invalid classes specified in the configuration.", e);
        }
        this.dtmfSink = new DtmfSink(scheduler, this, oobClock);

        // Register mixer components
        super.addAudioInput(this.rtpSource.getAudioInput());
        super.addAudioOutput(this.rtpSink.getAudioOutput());
        super.addOOBInput(this.dtmfSource.getOoBinput());
        super.addOOBOutput(this.dtmfSink.getOobOutput());

        // RTP statistics
        this.rxPackets = 0;

        // RTP transport
        this.rtpTransport = channel;
    }

    public void setRtpFormats(RTPFormats formats) {
        this.jitterBuffer.setFormats(formats);
    }

    public void processRtpPacket(RtpPacket packet, RTPFormat format) {
        if (this.rxPackets == 0) {
            logger.info("Restarting jitter buffer");
            this.jitterBuffer.restart();
        }
        this.rxPackets++;
        this.jitterBuffer.write(packet, format);
    }

    public void processDtmfPacket(RtpPacket packet) {

    }

    public void activate() {
        // activate media sink
        this.rtpSink.activate();
        this.dtmfSink.activate();

        // activate media source
        this.rtpSource.activate();
        this.dtmfSource.activate();
    }

    public void deactivate() {
        // deactivate media sink
        this.rtpSink.deactivate();
        this.dtmfSink.deactivate();
        this.dtmfSink.reset();

        // deactivate media source
        this.rtpSource.deactivate();
        this.dtmfSource.deactivate();
        this.dtmfSource.reset();
    }

    public void reset() {
        this.deactivate();
        this.dtmfSource.reset();
    }

    @Override
    public void incomingRtp(RtpPacket packet, RTPFormat format) {
        this.jitterBuffer.write(packet, format);
    }

    @Override
    public void outgoingRtp(RtpPacket packet) {
        try {
            this.rtpTransport.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void incomingDtmf(RtpPacket packet) {
        this.dtmfSource.write(packet);
    }

    @Override
    public void outgoingDtmf(RtpPacket packet) {
        try {
            this.rtpTransport.sendDtmf(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
