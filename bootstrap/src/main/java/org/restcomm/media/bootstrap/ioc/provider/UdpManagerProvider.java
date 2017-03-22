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
        
package org.restcomm.media.bootstrap.ioc.provider;

import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.scheduler.Scheduler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UdpManagerProvider implements Provider<UdpManager> {

    private final Scheduler scheduler;
    private final MediaServerConfiguration config;
    private final PortManager portManager;
    private final PortManager localPortManager;
    
    @Inject
    public UdpManagerProvider(MediaServerConfiguration config, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.config = config;
        this.portManager = new RtpPortManager(config.getMediaConfiguration().getLowPort(), config.getMediaConfiguration().getHighPort());
        this.localPortManager = new RtpPortManager();
    }
    
    @Override
    public UdpManager get() {
        UdpManager udpManager = new UdpManager(scheduler, this.portManager, this.localPortManager);
        udpManager.setBindAddress(config.getNetworkConfiguration().getBindAddress());
        udpManager.setLocalBindAddress(config.getControllerConfiguration().getAddress());
        udpManager.setExternalAddress(config.getNetworkConfiguration().getExternalAddress());
        udpManager.setLocalNetwork(config.getNetworkConfiguration().getNetwork());
        udpManager.setLocalSubnet(config.getNetworkConfiguration().getSubnet());
        udpManager.setUseSbc(config.getNetworkConfiguration().isSbc());
        udpManager.setRtpTimeout(config.getMediaConfiguration().getTimeout());
        return udpManager;
    }

}
