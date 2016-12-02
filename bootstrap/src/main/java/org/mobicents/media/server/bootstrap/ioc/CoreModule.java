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
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerFactoryProvider.AudioPlayerFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioPlayerPoolProvider.AudioPlayerPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderFactoryProvider.AudioRecorderFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.AudioRecorderPoolProvider.AudioRecorderPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.CachedRemoteStreamProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DirectRemoteStreamProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtlsSrtpServerProviderProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorFactoryProvider.DtmfDetectorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfDetectorPoolProvider.DtmfDetectorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorFactoryProvider.DtmfGeneratorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.DtmfGeneratorPoolProvider.DtmfGeneratorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.EndpointInstallerListProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.EndpointInstallerListProvider.EndpointInstallerListType;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionFactoryProvider.LocalConnectionFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.LocalConnectionPoolProvider.LocalConnectionPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.MediaSchedulerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorFactoryProvider.PhoneSignalDetectorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalDetectorPoolProvider.PhoneSignalDetectorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorFactoryProvider.PhoneSignalGeneratorFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.PhoneSignalGeneratorPoolProvider.PhoneSignalGeneratorPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.ResourcesPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionFactoryProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionFactoryProvider.RtpConnectionFactoryType;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionPoolProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.RtpConnectionPoolProvider.RtpConnectionPoolType;
import org.mobicents.media.server.bootstrap.ioc.provider.TaskSchedulerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.UdpManagerProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.WallClockProvider;
import org.mobicents.media.server.bootstrap.ioc.provider.mgcp.Mgcp2ControllerProvider;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.RemoteStreamProvider;
import org.mobicents.media.server.impl.rtp.crypto.DtlsSrtpServerProvider;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ServerManager;

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
        bind(RtpConnectionFactoryType.INSTANCE).toProvider(RtpConnectionFactoryProvider.class).in(Singleton.class);
        bind(RtpConnectionPoolType.INSTANCE).toProvider(RtpConnectionPoolProvider.class).in(Singleton.class);
        bind(LocalConnectionFactoryType.INSTANCE).toProvider(LocalConnectionFactoryProvider.class).in(Singleton.class);
        bind(LocalConnectionPoolType.INSTANCE).toProvider(LocalConnectionPoolProvider.class).in(Singleton.class);
        bind(AudioPlayerFactoryType.INSTANCE).toProvider(AudioPlayerFactoryProvider.class).in(Singleton.class);
        bind(AudioPlayerPoolType.INSTANCE).toProvider(AudioPlayerPoolProvider.class).in(Singleton.class);
        bind(AudioRecorderFactoryType.INSTANCE).toProvider(AudioRecorderFactoryProvider.class).in(Singleton.class);
        bind(AudioRecorderPoolType.INSTANCE).toProvider(AudioRecorderPoolProvider.class).in(Singleton.class);
        bind(DtmfDetectorFactoryType.INSTANCE).toProvider(DtmfDetectorFactoryProvider.class).in(Singleton.class);
        bind(DtmfDetectorPoolType.INSTANCE).toProvider(DtmfDetectorPoolProvider.class).in(Singleton.class);
        bind(DtmfGeneratorFactoryType.INSTANCE).toProvider(DtmfGeneratorFactoryProvider.class).in(Singleton.class);
        bind(DtmfGeneratorPoolType.INSTANCE).toProvider(DtmfGeneratorPoolProvider.class).in(Singleton.class);
        bind(PhoneSignalDetectorFactoryType.INSTANCE).toProvider(PhoneSignalDetectorFactoryProvider.class).in(Singleton.class);
        bind(PhoneSignalDetectorPoolType.INSTANCE).toProvider(PhoneSignalDetectorPoolProvider.class).in(Singleton.class);
        bind(PhoneSignalGeneratorFactoryType.INSTANCE).toProvider(PhoneSignalGeneratorFactoryProvider.class).in(Singleton.class);
        bind(PhoneSignalGeneratorPoolType.INSTANCE).toProvider(PhoneSignalGeneratorPoolProvider.class).in(Singleton.class);
        bind(ResourcesPool.class).toProvider(ResourcesPoolProvider.class).in(Singleton.class);
        bind(EndpointInstallerListType.INSTANCE).toProvider(EndpointInstallerListProvider.class).in(Singleton.class);
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
