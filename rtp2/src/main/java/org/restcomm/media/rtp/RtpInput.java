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

import org.apache.log4j.Logger;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.rtp.format.LinearFormat;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.jitter.JitterBufferEvent;
import org.restcomm.media.rtp.jitter.JitterBufferObserver;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dsp.Processor;
import org.restcomm.media.spi.memory.Frame;

/**
 * Media sink of RTP data coming from network.
 *
 * @author oifa yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInput extends AbstractSource implements JitterBufferObserver {

    private static final long serialVersionUID = 2537785080526404257L;

    private static final Logger log = Logger.getLogger(RtpInput.class);

    private final JitterBuffer buffer;
    private final Processor dsp;
    private final AudioInput input;

    public RtpInput(String name, PriorityQueueScheduler scheduler, JitterBuffer buffer, Processor dsp, AudioInput input) {
        super(name, scheduler, PriorityQueueScheduler.INPUT_QUEUE);
        this.buffer = buffer;
        this.dsp = dsp;
        this.input = input;
        buffer.observe(this);
        connect(this.input);
    }

    @Override
    public Frame evolve(long timestamp) {
        // Read from buffer
        Frame frame = this.buffer.read(timestamp);

        // Transcode frame to linear format
        if (frame != null) {
            try {
                frame = this.dsp.process(frame, frame.getFormat(), LinearFormat.FORMAT);
            } catch (Exception e) {
                final String fromFormat = frame.getFormat().getName().toString();
                final String toFormat = LinearFormat.FORMAT.getName().toString();
                log.warn("RTP Input " + getName() + " could not transcode a frame from " + fromFormat + " to " + toFormat, e);
            }
        }
        return frame;
    }

    @Override
    public void onJitterBufferEvent(JitterBuffer originator, JitterBufferEvent event) {
        switch (event) {
            case BUFFER_FILLED:
                // Jitter Buffer is filled. Resume transmission to consumer.
                this.wakeup();
                break;

            default:
                // Not interested in other events
                break;
        }

    }

}
