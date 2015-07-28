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
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.impl.rtp.channels.RtpChannel;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSink;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMixerComponent extends MediaComponent implements RtpRelay {

    private static final Logger logger = Logger.getLogger(RtpMixerComponent.class);

    private final static int DEFAULT_BUFFER_SIZER = 50;

    // Media mixing
    private final RtpSink rtpSink;
    private final RtpSource rtpSource;
    private final DtmfSink dtmfSink;
    private final DtmfSource dtmfSource;
    private final JitterBuffer jitterBuffer;

    // RTP transport
    private final RtpTransport rtpTransport;

    // RTP statistics
    private volatile int rxPackets;
    private volatile int sequenceNumber;

    public RtpMixerComponent(int channelId, Scheduler scheduler, DspFactory dspFactory, RtpTransport rtpTransport,
            RtpClock rtpClock, RtpClock oobClock) {
        super(channelId);

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
        addAudioInput(this.rtpSource.getAudioInput());
        addAudioOutput(this.rtpSink.getAudioOutput());
        addOOBInput(this.dtmfSource.getOoBinput());
        addOOBOutput(this.dtmfSink.getOobOutput());

        // RTP transport
        this.rtpTransport = rtpTransport;

        // RTP statistics
        this.rxPackets = 0;
        this.sequenceNumber = 0;
    }

    public void setRtpFormats(RTPFormats formats) {
        this.rtpSink.setFormats(formats);
    }
    
    private void activateSources() {
        this.rtpSource.activate();
        this.dtmfSource.activate();
    }

    private void deactivateSources() {
        this.rtpSource.deactivate();
        this.dtmfSource.deactivate();
        this.dtmfSource.reset();
    }
    
    private void activateSinks() {
        this.rtpSink.activate();
        this.dtmfSink.activate();
    }

    private void deactivateSinks() {
        this.rtpSink.deactivate();
        this.dtmfSink.deactivate();
        this.dtmfSink.reset();
    }

    @Override
    public void incomingRtp(RtpPacket packet, RTPFormat format) {
        // Determine whether the RTP packet is DTMF or not
        // and send it to the according media source
        if (RtpChannel.DTMF_FORMAT.matches(format.getFormat())) {
            this.dtmfSource.write(packet);
        } else {
            if (this.rxPackets == 0) {
                logger.info("Restarting jitter buffer");
                this.jitterBuffer.restart();
            }
            this.rxPackets++;
            this.jitterBuffer.write(packet, format);
        }
    }

    @Override
    public void outgoingRtp(RtpPacket packet) {
        try {
            // Increment sequence number
            int nextSequence = this.sequenceNumber++;
            packet.setSequenceNumber(nextSequence);

            // Adjust SSRC of the packet
            packet.setSyncSource(this.rtpTransport.getSsrc());

            // Send packet to remote peer
            this.rtpTransport.send(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void outgoingDtmf(RtpPacket packet) {
        try {
            // Increment sequence number
            int nextSequence = this.sequenceNumber++;
            packet.setSequenceNumber(nextSequence);

            // Adjust SSRC of the packet
            packet.setSyncSource(this.rtpTransport.getSsrc());

            // Send packet to remote peer
            this.rtpTransport.sendDtmf(packet);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void setMode(ConnectionMode mode) {
        switch (mode) {
            case SEND_ONLY:
                getAudioComponent().updateMode(false, true);
                getOOBComponent().updateMode(false, true);
                deactivateSources();
                activateSinks();
                break;

            case RECV_ONLY:
                getAudioComponent().updateMode(true, false);
                getOOBComponent().updateMode(true, false);
                activateSources();
                deactivateSinks();
                break;

            case SEND_RECV:
            case CONFERENCE:
                getAudioComponent().updateMode(true, true);
                getOOBComponent().updateMode(true, true);
                activateSinks();
                activateSources();
                break;

            case NETWORK_LOOPBACK:
            case INACTIVE:
                getAudioComponent().updateMode(false, false);
                getOOBComponent().updateMode(false, false);
                deactivateSinks();
                deactivateSources();
                break;

            default:
                break;
        }
    }

}
