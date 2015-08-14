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
import org.mobicents.media.server.impl.rtp.channels.RtpSession;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSink;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.dsp.Processor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpComponent extends MediaComponent implements RtpRelay {

    private static final Logger logger = Logger.getLogger(RtpComponent.class);

    private final static int DEFAULT_BUFFER_SIZER = 50;

    static final RTPFormats EMPTY_FORMATS = new RTPFormats(0);

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

    public RtpComponent(int channelId, Scheduler scheduler, RtpTransport rtpTransport, RtpClock rtpClock, RtpClock oobClock,
            Processor transcoder) {
        super(channelId, transcoder);

        // RTP source
        this.jitterBuffer = new JitterBuffer(rtpClock, DEFAULT_BUFFER_SIZER);
        this.rtpSource = new RtpSource(scheduler, jitterBuffer);
        this.dtmfSource = new DtmfSource(scheduler, oobClock);

        // RTP sink
        this.rtpSink = new RtpSink(scheduler, rtpClock, this, transcoder);
        this.dtmfSink = new DtmfSink(scheduler, this, oobClock);

        // Register mixer components
        addInput(this.rtpSource.getMediaInput());
        addOutput(this.rtpSink.getMediaOutput());
        addOOBInput(this.dtmfSource.getOoBinput());
        addOOBOutput(this.dtmfSink.getOobOutput());

        // RTP transport
        this.rtpTransport = rtpTransport;

        // RTP statistics
        this.rxPackets = 0;
        this.sequenceNumber = 0;
    }

    @Override
    public void setFormats(RTPFormats formats) {
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
        if (RtpSession.DTMF_FORMAT.matches(format.getFormat())) {
            this.dtmfSource.write(packet);
        } else {
            // Tell the media sink what is the format the remote peer is expecting
            this.rtpSink.setCurrentFormat(format);

            // Place packet in the jitter buffer
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
            logger.warn("RTP packet dropped: " + e.getMessage(), e);
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
            logger.warn("DTMF packet dropped: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateMode(ConnectionMode connectionMode) {
        super.updateMode(connectionMode);
        switch (connectionMode) {
            case SEND_ONLY:
                deactivateSources();
                activateSinks();
                break;

            case RECV_ONLY:
                activateSources();
                deactivateSinks();
                break;

            case SEND_RECV:
            case CONFERENCE:
                activateSinks();
                activateSources();
                break;

            case NETWORK_LOOPBACK:
            case INACTIVE:
            default:
                deactivateSinks();
                deactivateSources();
                break;
        }
    }

}
