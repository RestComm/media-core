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

package org.restcomm.media.bootstrap.ioc.provider;

import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.resources.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resources.dtmf.GeneratorImpl;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.spi.pooling.PooledObjectFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DtmfGeneratorFactoryProvider implements Provider<DtmfGeneratorFactory> {

    private final MediaServerConfiguration config;
    private final PriorityQueueScheduler mediaScheduler;

    @Inject
    public DtmfGeneratorFactoryProvider(MediaServerConfiguration config, PriorityQueueScheduler mediaScheduler) {
        this.config = config;
        this.mediaScheduler = mediaScheduler;
    }

    @Override
    public DtmfGeneratorFactory get() {
        int duration = this.config.getResourcesConfiguration().getDtmfGeneratorToneDuration();
        int volume = this.config.getResourcesConfiguration().getDtmfGeneratorToneVolume();
        return new DtmfGeneratorFactory(mediaScheduler, volume, duration);
    }

    public static final class DtmfGeneratorFactoryType extends TypeLiteral<PooledObjectFactory<GeneratorImpl>> {

        public static final DtmfGeneratorFactoryType INSTANCE = new DtmfGeneratorFactoryType();

        private DtmfGeneratorFactoryType() {
            super();
        }

    }

}
