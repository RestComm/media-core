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

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;
import org.mobicents.media.server.spi.dsp.DspFactory;

import com.google.inject.Inject;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServer implements MediaServer {
    
    private static final Logger log = Logger.getLogger(RestCommMediaServer.class);
    
    // Core Components
    private final Clock clock;
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler taskScheduler;
    private final UdpManager udpManager;
    private final ChannelsManager channelsManager;
    private final DspFactory dspFactory;
    private final ServerManager controller;

    @Inject
    public RestCommMediaServer(Clock clock, PriorityQueueScheduler mediaScheduler, Scheduler taskScheduler, UdpManager udpManager, ChannelsManager channelsManager, DspFactory dspFactory, ServerManager controller) {
        this.clock = clock;
        this.mediaScheduler = mediaScheduler;
        this.taskScheduler = taskScheduler;
        this.udpManager = udpManager;
        this.channelsManager = channelsManager;
        this.dspFactory = dspFactory;
        this.controller = controller;
    }

    @Override
    public void addManager(ServerManager manager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeManager(ServerManager manager) {
        // TODO Auto-generated method stub

    }

    @Override
    public void start() throws IllegalStateException {
        log.info("Media Server started!!!");
        
    }

    @Override
    public void stop() throws IllegalStateException {
        log.info("Media Server stopped!!!");
    }

}
