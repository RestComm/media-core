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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dsp.DspFactory;
import org.restcomm.media.spi.dsp.Processor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpOutputFactory {

    private static final Logger log = Logger.getLogger(RtpOutputFactory.class);

    static final String NAME_PREFIX = "rtp-output";
    static final int MIN_ID = 0;
    static final int MAX_ID = 1000000;

    // Core Components
    private final PriorityQueueScheduler scheduler;
    private final DspFactory dspFactory;

    // RTP Output Factory
    private final AtomicInteger idGenerator;

    public RtpOutputFactory(PriorityQueueScheduler scheduler, DspFactory dspFactory) {
        super();
        this.scheduler = scheduler;
        this.dspFactory = dspFactory;
        this.idGenerator = new AtomicInteger(MIN_ID);
    }

    public RtpOutput build() {
        // Retrieve dependencies
        int id = generateId();
        String name = NAME_PREFIX + "-" + id;
        AudioOutput audioOutput = new AudioOutput(scheduler, id);
        Processor dsp;
        try {
            dsp = this.dspFactory.newProcessor();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            dsp = null;
            log.error("Could not create DSP for RTP Output " + id, e);
        }

        // Build RTP Output
        RtpOutput rtpOutput = new RtpOutput(name, audioOutput, dsp);
        return rtpOutput;
    }

    private int generateId() {
        int nextId = this.idGenerator.incrementAndGet();
        this.idGenerator.compareAndSet(MAX_ID, MIN_ID);
        return nextId;
    }

}
