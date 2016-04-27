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

package org.mobicents.media.server.bootstrap.ioc.provider;

import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.controller.Controller;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.EndpointInstaller;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpControllerProvider implements Provider<Controller> {
    
    private static final Logger log = Logger.getLogger(MgcpControllerProvider.class);

    private final MediaServerConfiguration config;
    private final UdpManager udpManager;
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler taskScheduler;
    private final ResourcesPool resourcesPool;
    private final List<EndpointInstaller> endpointInstallers;

    @Inject
    public MgcpControllerProvider(MediaServerConfiguration config, UdpManager udpManager, PriorityQueueScheduler mediaScheduler, Scheduler taskScheduler, 
            ResourcesPool resourcesPool, List<EndpointInstaller> endpointInstallers) {
        this.config = config;
        this.udpManager = udpManager;
        this.mediaScheduler = mediaScheduler;
        this.taskScheduler = taskScheduler;
        this.resourcesPool = resourcesPool;
        this.endpointInstallers = endpointInstallers;
    }

    @Override
    public Controller get() {
        Controller controller = new Controller();
        controller.setUdpInterface(this.udpManager);
        controller.setMediaScheduler(this.mediaScheduler);
        controller.setTaskScheduler(this.taskScheduler);
        controller.setResourcesPool(this.resourcesPool);
        controller.setPort(config.getControllerConfiguration().getPort());
        controller.setPoolSize(config.getControllerConfiguration().getPoolSize());
        try {
            controller.setConfiguration(config.getControllerConfiguration().getConfiguration());
        } catch (Exception e) {
            log.error("Could not load configuration of MGCP controller", e);
        }
        for (EndpointInstaller installer : endpointInstallers) {
            controller.addInstaller(installer);
        }
        return controller;
    }

}
