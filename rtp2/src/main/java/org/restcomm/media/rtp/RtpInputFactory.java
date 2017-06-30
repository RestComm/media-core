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
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.rtp.format.LinearFormat;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dsp.DspFactory;
import org.restcomm.media.spi.dsp.Processor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInputFactory {

    private static final Logger log = Logger.getLogger(RtpInputFactory.class);

    static final String NAME_PREFIX = "rtp-input";
    static final int MIN_ID = 0;
    static final int MAX_ID = 1000000;

    // Core Components
    private final PriorityQueueScheduler scheduler;
    private final DspFactory dspFactory;
    private final JitterBufferFactory jitterBufferFactory;

    // RTP Input Factory
    private final AtomicInteger idGenerator;

    public RtpInputFactory(PriorityQueueScheduler scheduler, DspFactory dspFactory, JitterBufferFactory jitterBufferFactory) {
        super();
        this.scheduler = scheduler;
        this.dspFactory = dspFactory;
        this.jitterBufferFactory = jitterBufferFactory;
        this.idGenerator = new AtomicInteger(MIN_ID);
    }

    public RtpInput build() {
        int id = generateId();
        String name = NAME_PREFIX + "-" + id;
        JitterBuffer jitterBuffer = this.jitterBufferFactory.build();
        AudioInput audioInput = new AudioInput(id, LinearFormat.PACKET_SIZE);
        Processor dsp;
        try {
            dsp = this.dspFactory.newProcessor();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            dsp = null;
            log.error("Could not create DSP for RTP Input " + id, e);
        }

        RtpInput rtpInput = new RtpInput(name, scheduler, jitterBuffer, dsp, audioInput);
        return rtpInput;
    }

    private int generateId() {
        int nextId = this.idGenerator.incrementAndGet();
        this.idGenerator.compareAndSet(MAX_ID, MIN_ID);
        return nextId;
    }

}
