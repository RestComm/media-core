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

import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.spi.MediaServer;
import org.mobicents.media.server.spi.ServerManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestCommMediaServer implements MediaServer {
    
    private final Clock clock;
    private final PriorityQueueScheduler mediaScheduler;
    private final ServiceScheduler taskScheduler;
    private final UdpManager udpManager;
    
    public RestCommMediaServer(MediaServerConfiguration configuration) {
        this.clock = new WallClock();
        this.mediaScheduler = new PriorityQueueScheduler();
        // TODO setup media scheduler
        this.taskScheduler = new ServiceScheduler(this.clock);
        this.udpManager = new UdpManager(this.taskScheduler);
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
