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
import org.mobicents.media.core.configuration.MgcpControllerConfiguration;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.controller.Controller;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.MediaServer;

/**
 * Providers Controllers for the Media Server.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ControllerProvider {

    private static final Logger log = Logger.getLogger(ControllerProvider.class);

    public static Controller buildMgcpController(MgcpControllerConfiguration config, UdpManager udpManager, PriorityQueueScheduler mediaScheduler, Scheduler taskScheduler, MediaServer mediaServer) {
        Controller obj = new Controller();
        obj.setUdpInterface(udpManager);
        obj.setMediaScheduler(mediaScheduler);
        obj.setTaskScheduler(taskScheduler);
        // obj.setServer(mediaServer);
        obj.setPort(config.getPort());
        obj.setPoolSize(config.getPoolSize());
        try {
            obj.setConfiguration(config.getConfiguration());
        } catch (Exception e) {
            log.error("Could not load MGCP controller configuration", e);
        }
        return obj;
    }

}
