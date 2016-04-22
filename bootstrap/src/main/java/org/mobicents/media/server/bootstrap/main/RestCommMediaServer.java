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

import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.core.connections.LocalConnectionImpl;
import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetector;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGenerator;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;
import org.mobicents.media.server.spi.dsp.DspFactory;
import org.mobicents.media.server.spi.pooling.ResourcePool;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServer implements MediaServer {

    // Core Components
    private final Clock clock;
    private final PriorityQueueScheduler mediaScheduler;
    private final ServiceScheduler taskScheduler;
    private final UdpManager udpManager;
    private final ChannelsManager channelsManager;
    private final DspFactory dspFactory;
    private final ServerManager controller;

    // Resource Pools
    private final ResourcePool<RtpConnectionImpl> rtpConnectionPool;
    private final ResourcePool<LocalConnectionImpl> localConnectionPool;
    private final ResourcePool<AudioPlayerImpl> playerPool;
    private final ResourcePool<AudioRecorderImpl> recorderPool;
    private final ResourcePool<GeneratorImpl> dtmfGeneratorPool;
    private final ResourcePool<DetectorImpl> dtmfDetectorPool;
    private final ResourcePool<PhoneSignalDetector> signalDetectorPool;
    private final ResourcePool<PhoneSignalGenerator> signalGeneratorPool;
    private final ResourcesPool resourcesPool;

    public RestCommMediaServer(MediaServerConfiguration configuration) {
        // Core Components
        this.clock = new WallClock();
        this.mediaScheduler = new PriorityQueueScheduler(clock);
        this.taskScheduler = new ServiceScheduler(clock);
        this.udpManager = UdpManagerFactory.build(configuration, taskScheduler);
        this.channelsManager = ChannelsManagerFactory.build(configuration, udpManager, mediaScheduler);
        this.dspFactory = DspProvider.build(configuration);
        this.controller = ControllerProvider.buildMgcpController(configuration.getControllerConfiguration(), udpManager, mediaScheduler, taskScheduler, this);

        // Resource Pools
        this.rtpConnectionPool = ResourcePoolProvider.buildRtpConnectionPool(configuration.getResourcesConfiguration(), channelsManager, dspFactory);
        this.localConnectionPool = ResourcePoolProvider.buildLocalConnectionPool(configuration.getResourcesConfiguration(), channelsManager);
        this.playerPool = ResourcePoolProvider.buildPlayerPool(configuration.getResourcesConfiguration(), mediaScheduler, dspFactory);
        this.recorderPool = ResourcePoolProvider.buildRecorderPool(configuration.getResourcesConfiguration(), mediaScheduler);
        this.dtmfGeneratorPool = ResourcePoolProvider.buildDtmfGeneratorPool(configuration.getResourcesConfiguration(), mediaScheduler);
        this.dtmfDetectorPool = ResourcePoolProvider.buildDtmfDetectorPool(configuration.getResourcesConfiguration(), mediaScheduler);
        this.signalDetectorPool = ResourcePoolProvider.buildSignalDetectorPool(configuration.getResourcesConfiguration(), mediaScheduler);
        this.signalGeneratorPool = ResourcePoolProvider.buildSignalGeneratorPool(configuration.getResourcesConfiguration(), mediaScheduler);
        this.resourcesPool = new ResourcesPool(rtpConnectionPool, localConnectionPool, playerPool, recorderPool, dtmfDetectorPool, dtmfGeneratorPool, signalDetectorPool, signalGeneratorPool);
    }

    @Override
    public void addManager(ServerManager manager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeManager(ServerManager manager) {
        // TODO Auto-generated method stub

    }

}
