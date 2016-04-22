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

package org.mobicents.media.server.bootstrap.main;

import org.mobicents.media.core.configuration.ResourcesConfiguration;
import org.mobicents.media.core.connections.LocalConnectionFactory;
import org.mobicents.media.core.connections.LocalConnectionImpl;
import org.mobicents.media.core.connections.LocalConnectionPool;
import org.mobicents.media.core.connections.RtpConnectionFactory;
import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.core.connections.RtpConnectionPool;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderFactory;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderPool;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorPool;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorPool;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.pooling.PooledObjectFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;

/**
 * Provides resource pools.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ResourcePoolProvider {

    public static ResourcePool<RtpConnectionImpl> buildRtpConnectionPool(ResourcesConfiguration config,
            ChannelsManager channelsManager, DspFactory dspFactory) {
        PooledObjectFactory<RtpConnectionImpl> factory = new RtpConnectionFactory(channelsManager, dspFactory);
        return new RtpConnectionPool(config.getRemoteConnectionCount(), factory);
    }

    public static ResourcePool<LocalConnectionImpl> buildLocalConnectionPool(ResourcesConfiguration config,
            ChannelsManager channelsManager) {
        PooledObjectFactory<LocalConnectionImpl> factory = new LocalConnectionFactory(channelsManager);
        return new LocalConnectionPool(config.getLocalConnectionCount(), factory);
    }

    public static ResourcePool<AudioPlayerImpl> buildPlayerPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler, DspFactory dspFactory) {
        PooledObjectFactory<AudioPlayerImpl> factory = new AudioPlayerFactory(mediaScheduler, dspFactory);
        return new AudioPlayerPool(config.getPlayerCount(), factory);
    }

    public static ResourcePool<AudioRecorderImpl> buildRecorderPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler) {
        PooledObjectFactory<AudioRecorderImpl> factory = new AudioRecorderFactory(mediaScheduler);
        return new AudioRecorderPool(config.getRecorderCount(), factory);
    }

    public static ResourcePool<GeneratorImpl> buildDtmfGeneratorPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler) {
        PooledObjectFactory<GeneratorImpl> factory = new DtmfGeneratorFactory(mediaScheduler,
                config.getDtmfGeneratorToneVolume(), config.getDtmfGeneratorToneDuration());
        return new DtmfGeneratorPool(config.getDtmfGeneratorCount(), factory);
    }

    public static ResourcePool<DetectorImpl> buildDtmfDetectorPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler) {
        PooledObjectFactory<DetectorImpl> factory = new DtmfDetectorFactory(mediaScheduler, config.getDtmfDetectorDbi());
        return new DtmfDetectorPool(config.getDtmfDetectorCount(), factory);
    }

    public static ResourcePool<PhoneSignalGenerator> buildSignalGeneratorPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler) {
        PooledObjectFactory<PhoneSignalGenerator> factory = new PhoneSignalGeneratorFactory(mediaScheduler);
        return new PhoneSignalGeneratorPool(config.getSignalGeneratorCount(), factory);
    }

    public static ResourcePool<PhoneSignalDetector> buildSignalDetectorPool(ResourcesConfiguration config,
            PriorityQueueScheduler mediaScheduler) {
        PooledObjectFactory<PhoneSignalDetector> factory = new PhoneSignalDetectorFactory(mediaScheduler);
        return new PhoneSignalDetectorPool(config.getSignalDetectorCount(), factory);
    }

}
