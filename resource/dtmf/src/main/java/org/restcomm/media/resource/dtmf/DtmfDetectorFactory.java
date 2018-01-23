/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.resource.dtmf;

import java.util.concurrent.atomic.AtomicInteger;

import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.dtmf.DtmfDetector;
import org.restcomm.media.spi.pooling.PooledObjectFactory;

/**
 * Factory that produces DTMF Detectors.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class DtmfDetectorFactory implements PooledObjectFactory<DetectorImpl> {

    /** Global ID generator for DTMF detectors */
    private static final AtomicInteger ID = new AtomicInteger(1);

    private final PriorityQueueScheduler mediaScheduler;
    private final int volume;
    private final int duration;
    private final int interval;

    public DtmfDetectorFactory(PriorityQueueScheduler mediaScheduler, int volume, int duration, int interval) {
        this.mediaScheduler = mediaScheduler;
        this.volume = volume;
        this.duration = duration;
        this.interval = interval;
    }

    public DtmfDetectorFactory(PriorityQueueScheduler mediaScheduler) {
        this(mediaScheduler, DtmfDetector.DEFAULT_SIGNAL_LEVEL, DtmfDetector.DEFAULT_SIGNAL_DURATION, DtmfDetector.DEFAULT_INTERDIGIT_INTERVAL);
    }

    @Override
    public DetectorImpl produce() {
        return new DetectorImpl("detector-" + ID.getAndIncrement(), this.volume, this.duration, this.interval, mediaScheduler);
    }

}
