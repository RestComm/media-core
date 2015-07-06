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

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.AudioInput;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dsp.Processor;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Implementation of a media source.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * @author Oifa Yulian
 *
 */
public class RtpSource extends AbstractSource implements BufferListener {

    private static final long serialVersionUID = -434214678421348922L;

    private static final Logger logger = Logger.getLogger(RtpSource.class);

    // Output format
    private static final AudioFormat LINEAR_FORMAT = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private static final long PERIOD = 20000000L;
    private static final int PACKET_SIZE = (int) (PERIOD / 1000000) * LINEAR_FORMAT.getSampleRate() / 1000
            * LINEAR_FORMAT.getSampleSize() / 8;

    // Media mixing components
    private final Processor dsp;
    private final JitterBuffer jitterBuffer;
    private final AudioInput audioInput;

    public RtpSource(Scheduler scheduler, JitterBuffer jitterBuffer, Processor dsp) {
        super("input", scheduler, Scheduler.INPUT_QUEUE);

        // Media mixing components
        this.jitterBuffer = jitterBuffer;
        this.dsp = dsp;
        this.audioInput = new AudioInput(1, PACKET_SIZE);
        super.connect(audioInput);
    }

    public AudioInput getAudioInput() {
        return audioInput;
    }

    @Override
    public Frame evolve(long timestamp) {
        Frame currFrame = this.jitterBuffer.read(timestamp);
        if (currFrame != null && dsp != null) {
            try {
                currFrame = dsp.process(currFrame, currFrame.getFormat(), LINEAR_FORMAT);
            } catch (Exception e) {
                currFrame = null;
                logger.error("Transcoding error: " + e.getMessage(), e);
            }
        }
        return currFrame;
    }

    @Override
    public void onFill() {
        this.wakeup();
    }

}
