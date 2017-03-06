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

package org.restcomm.media.server.bootstrap.ioc.provider;

import org.mobicents.media.server.spi.pooling.PooledObjectFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.resource.player.audio.AudioPlayerImpl;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioPlayerPoolProvider implements Provider<AudioPlayerPool> {

    private final MediaServerConfiguration config;
    private final PooledObjectFactory<AudioPlayerImpl> factory;

    @Inject
    public AudioPlayerPoolProvider(MediaServerConfiguration config, PooledObjectFactory<AudioPlayerImpl> factory) {
        this.config = config;
        this.factory = factory;
    }

    @Override
    public AudioPlayerPool get() {
        return new AudioPlayerPool(this.config.getResourcesConfiguration().getPlayerCount(), this.factory);
    }

    public static final class AudioPlayerPoolType extends TypeLiteral<ResourcePool<AudioPlayerImpl>> {

        public static final AudioPlayerPoolType INSTANCE = new AudioPlayerPoolType();

        private AudioPlayerPoolType() {
            super();
        }

    }

}
