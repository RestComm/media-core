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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.rtp.format.LinearFormat;
import org.restcomm.media.rtp.session.RtpSessionContext;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.memory.Frame;

import javax.sound.sampled.Line;

/**
 * Media source of RTP data going to the network.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Yulian Oifa
 */
public class RtpOutput extends AbstractSink implements MediaSourceSubject {

    private static final long serialVersionUID = -7726485962772259820L;

    private static final Logger log = LogManager.getLogger(RtpOutput.class);

    private final AudioOutput output;
    private final Processor dsp;
    private final RtpSessionContext sessionContext;

    private final Set<MediaSourceObserver> observers;

    public RtpOutput(String name, RtpSessionContext sessionContext, AudioOutput output, Processor dsp) {
        super(name);
        this.sessionContext = sessionContext;
        this.dsp = dsp;
        this.output = output;
        this.output.join(this);

        this.observers = Sets.newHashSet();
    }

    @Override
    public void activate() {
        this.output.start();
    }

    @Override
    public void deactivate() {
        this.output.stop();
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        try {
            // Transcode fram from LINEAR format to current session format
            final RTPFormat currentFormat = sessionContext.getCurrentFormat();
            final Frame transcodedFrame = this.dsp.process(frame, LinearFormat.FORMAT, currentFormat.getFormat());

            // TODO adapt rtp clock rate

            // Convert timestamp to milliseconds
            final long timestampMillis = frame.getTimestamp() / 1000000L;

            // Ignore frames with duplicate time frame
            if(timestampMillis == sessionContext.getTxTimestamp()) {
                transcodedFrame.recycle();
                return;
            }

            // Convert timestamp to RTP time units
            final long timestampRtp = this.sessionContext.getRtpClock().convertToRtpTime(timestampMillis);

            // Increment sequence number
            final int sequence = this.sessionContext.getTxSequence().incrementAndGet();

            // Construct RTP packet from Frame
            final RtpPacket rtpPacket = new RtpPacket(false, currentFormat.getID(), sequence, timestampRtp, sessionContext.getSsrc(), transcodedFrame.getData());

            // Recycle frame
            transcodedFrame.recycle();

            notify(rtpPacket);
        } catch (Exception e) {
            log.error("RTP Output from session " + sessionContext.getSsrc() + " could not transcode frame", e);
        }
    }

    public AudioOutput getOutput() {
        return output;
    }

    @Override
    public void observe(MediaSourceObserver observer) {
        final boolean added = this.observers.add(observer);
        if (added && log.isDebugEnabled()) {
            log.debug("RTP Output " + sessionContext.getSsrc() + " registered observer MediaSourceObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(MediaSourceObserver observer) {
        final boolean removed = this.observers.remove(observer);
        if (removed && log.isDebugEnabled()) {
            log.debug("RTP Output " + sessionContext.getSsrc() + " unregistered observer MediaSourceObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(RtpPacket rtpPacket) {
        for (MediaSourceObserver observer : this.observers) {
            observer.onMediaGenerated(rtpPacket);
        }
    }

}
