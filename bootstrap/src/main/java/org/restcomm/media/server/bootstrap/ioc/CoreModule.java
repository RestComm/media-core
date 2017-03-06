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

package org.restcomm.media.server.bootstrap.ioc;

import org.mobicents.media.server.impl.resource.mediaplayer.audio.RemoteStreamProvider;
import org.mobicents.media.server.impl.rtp.crypto.DtlsSrtpServerProvider;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ServerManager;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioPlayerFactoryProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioPlayerPoolProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioRecorderFactoryProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioRecorderPoolProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.CachedRemoteStreamProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DirectRemoteStreamProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DtlsSrtpServerProviderProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfDetectorFactoryProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfDetectorPoolProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfGeneratorFactoryProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfGeneratorPoolProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.MediaSchedulerProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.TaskSchedulerProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.UdpManagerProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.WallClockProvider;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioPlayerFactoryProvider.AudioPlayerFactoryType;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioPlayerPoolProvider.AudioPlayerPoolType;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioRecorderFactoryProvider.AudioRecorderFactoryType;
import org.restcomm.media.server.bootstrap.ioc.provider.AudioRecorderPoolProvider.AudioRecorderPoolType;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfDetectorFactoryProvider.DtmfDetectorFactoryType;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfDetectorPoolProvider.DtmfDetectorPoolType;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfGeneratorFactoryProvider.DtmfGeneratorFactoryType;
import org.restcomm.media.server.bootstrap.ioc.provider.DtmfGeneratorPoolProvider.DtmfGeneratorPoolType;
import org.restcomm.media.server.bootstrap.ioc.provider.mgcp.Mgcp2ControllerProvider;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CoreModule extends AbstractModule {

    private final MediaServerConfiguration config;

    public CoreModule(MediaServerConfiguration config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(MediaServerConfiguration.class).toInstance(this.config);
        bind(Clock.class).toProvider(WallClockProvider.class).in(Singleton.class);
        bind(PriorityQueueScheduler.class).toProvider(MediaSchedulerProvider.class).in(Singleton.class);
        bind(Scheduler.class).toProvider(TaskSchedulerProvider.class).in(Singleton.class);
        bind(UdpManager.class).toProvider(UdpManagerProvider.class).in(Singleton.class);
        bind(AudioPlayerFactoryType.INSTANCE).toProvider(AudioPlayerFactoryProvider.class).in(Singleton.class);
        bind(AudioPlayerPoolType.INSTANCE).toProvider(AudioPlayerPoolProvider.class).in(Singleton.class);
        bind(AudioRecorderFactoryType.INSTANCE).toProvider(AudioRecorderFactoryProvider.class).in(Singleton.class);
        bind(AudioRecorderPoolType.INSTANCE).toProvider(AudioRecorderPoolProvider.class).in(Singleton.class);
        bind(DtmfDetectorFactoryType.INSTANCE).toProvider(DtmfDetectorFactoryProvider.class).in(Singleton.class);
        bind(DtmfDetectorPoolType.INSTANCE).toProvider(DtmfDetectorPoolProvider.class).in(Singleton.class);
        bind(DtmfGeneratorFactoryType.INSTANCE).toProvider(DtmfGeneratorFactoryProvider.class).in(Singleton.class);
        bind(DtmfGeneratorPoolType.INSTANCE).toProvider(DtmfGeneratorPoolProvider.class).in(Singleton.class);
        bind(ServerManager.class).toProvider(Mgcp2ControllerProvider.class).in(Singleton.class);
        bind(DtlsSrtpServerProvider.class).toProvider(DtlsSrtpServerProviderProvider.class).in(Singleton.class);
        Class<? extends Provider<? extends RemoteStreamProvider>> remoteStreamProvider;
        if (this.config.getResourcesConfiguration().getPlayerCacheEnabled()) {
            remoteStreamProvider = CachedRemoteStreamProvider.class;
        } else {
            remoteStreamProvider = DirectRemoteStreamProvider.class;
        }
        bind(RemoteStreamProvider.class).toProvider(remoteStreamProvider).in(Singleton.class);
    }

}
