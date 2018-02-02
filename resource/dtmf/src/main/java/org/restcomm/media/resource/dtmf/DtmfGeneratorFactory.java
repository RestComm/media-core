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
import org.restcomm.media.spi.pooling.PooledObjectFactory;

/**
 * Factory that produces DTMF Generators.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DtmfGeneratorFactory implements PooledObjectFactory<GeneratorImpl> {

    /** Global ID generator for produced objects */
    private static final AtomicInteger ID = new AtomicInteger(1);

    private static final int TONE_DURATION = 80;
    private static final int TONE_VOLUME = -20;

    private final PriorityQueueScheduler mediaScheduler;

    private int duration;
    private int volume;

    public DtmfGeneratorFactory(PriorityQueueScheduler mediaScheduler, int volume, int duration) {
        this.mediaScheduler = mediaScheduler;
        this.volume = volume;
        this.duration = duration;
    }

    public DtmfGeneratorFactory(PriorityQueueScheduler mediaScheduler) {
        this(mediaScheduler, TONE_VOLUME, TONE_DURATION);
    }

    @Override
    public GeneratorImpl produce() {
        GeneratorImpl generator = new GeneratorImpl("generator" + ID.getAndIncrement(), mediaScheduler);
        generator.setVolume(this.volume);
        generator.setToneDuration(this.duration);
        return generator;
    }

}
