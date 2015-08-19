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

import org.mobicents.media.server.component.MediaOutput;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implementation of a Media Sink.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSink extends AbstractSink {

    private static final long serialVersionUID = -3660168309653874674L;

    // Media processing components
    private final MediaOutput mediaOutput;
    private final Processor transcoder;

    // RTP processing components
    private final RtpClock rtpClock;
    private final RtpPacket rtpPacket;

    // RTP transport
    private final RtpComponent rtpGateway;
    private RTPFormats formats;

    // Details of last transmitted packet
    private RTPFormat currentFormat;
    private long rtpTimestamp;

    public RtpSink(Scheduler scheduler, RtpClock rtpClock, RtpComponent rtpGateway, Processor transcoder) {
        super("output");

        // Media processing components
        this.mediaOutput = new MediaOutput(1, scheduler);
        this.mediaOutput.join(this);
        this.transcoder = transcoder;

        // RTP processing components
        this.rtpClock = rtpClock;

        // RTP transport
        this.rtpGateway = rtpGateway;
        this.rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
        this.formats = RtpComponent.EMPTY_FORMATS;

        // Details of last transmitted packet
        this.rtpTimestamp = 0L;
    }

    public MediaOutput getMediaOutput() {
        return mediaOutput;
    }

    public void setFormats(RTPFormats formats) {
        this.formats = formats;
        // Set a default format
        if (this.currentFormat == null) {
            setCurrentFormat(formats.toArray()[0]);
        }
    }

    public void setCurrentFormat(RTPFormat format) throws IllegalArgumentException {
        if (!formats.contains(format.getID())) {
            throw new IllegalArgumentException("The codec is not supported: " + format.toString());
        }
        this.currentFormat = format;
        this.rtpClock.setClockRate(currentFormat.getClockRate());
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        // send the packet to remote peer if media frame is valid
        if (evolveFrame(frame)) {
            this.rtpGateway.outgoingRtp(this.rtpPacket);
        }
    }

    private boolean evolveFrame(Frame frame) {
        // discard frame if format is unknown
        if (frame.getFormat() == null) {
            frame.recycle();
            return false;
        }

        // determine current RTP format if it is unknown
        if (this.currentFormat == null) {
            RTPFormat newFormat = this.formats.getRTPFormat(frame.getFormat());
            if (newFormat == null) {
                // discard packet if format is still unknown
                frame.recycle();
                return false;
            } else {
                // update the current format and clock rate according to it
                this.currentFormat = newFormat;
                rtpClock.setClockRate(currentFormat.getClockRate());
            }
        }

        // perform transcoding if necessary
        if (!this.currentFormat.getFormat().matches(frame.getFormat())) {
            frame = transcoder.process(frame, frame.getFormat(), this.currentFormat.getFormat());
        }

        // convert frame timestamp to milliseconds
        long frameTimestamp = frame.getTimestamp() / 1000000L;

        // ignore frames with duplicate timestamp
        if (frameTimestamp == this.rtpTimestamp) {
            frame.recycle();
            return false;
        }

        this.rtpTimestamp = rtpClock.convertToRtpTime(frameTimestamp);

        // wrap the RTP packet and return it
        // NOTE 1: the SSRC field is unknown at this point, it must be overwritten by the RTP transport object!
        // NOTE 2: the sequence number field is unknown at this point, it must be overwritten by the RTP transport object!
        rtpPacket.wrap(false, currentFormat.getID(), 0, this.rtpTimestamp, 0L, frame.getData(), frame.getOffset(),
                frame.getLength());
        frame.recycle();
        return true;
    }

    @Override
    public void activate() {
        this.mediaOutput.start();
    }

    @Override
    public void deactivate() {
        this.mediaOutput.stop();
    }

}
