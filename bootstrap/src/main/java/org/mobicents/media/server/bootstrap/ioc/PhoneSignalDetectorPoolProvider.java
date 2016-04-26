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

package org.mobicents.media.server.bootstrap.ioc;

import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorPool;
import org.mobicents.media.server.spi.pooling.PooledObjectFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PhoneSignalDetectorPoolProvider implements Provider<PhoneSignalDetectorPool> {

    private final MediaServerConfiguration config;
    private final PooledObjectFactory<PhoneSignalDetector> factory;

    @Inject
    public PhoneSignalDetectorPoolProvider(MediaServerConfiguration config, PooledObjectFactory<PhoneSignalDetector> factory) {
        this.config = config;
        this.factory = factory;
    }

    @Override
    public PhoneSignalDetectorPool get() {
        return new PhoneSignalDetectorPool(config.getResourcesConfiguration().getSignalDetectorCount(), factory);
    }

    public static final class PhoneSignalDetectorPoolType extends TypeLiteral<ResourcePool<PhoneSignalDetector>> {

        public static final PhoneSignalDetectorPoolType INSTANCE = new PhoneSignalDetectorPoolType();

        private PhoneSignalDetectorPoolType() {
            super();
        }

    }

}
