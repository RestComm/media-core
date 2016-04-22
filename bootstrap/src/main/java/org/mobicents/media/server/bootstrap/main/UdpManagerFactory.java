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
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * Factory that builds UDP Managers
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UdpManagerFactory {

    public static UdpManager build(MediaServerConfiguration config, Scheduler scheduler) {
        UdpManager obj = new UdpManager(scheduler);
        // Configure network parameters
        obj.setBindAddress(config.getNetworkConfiguration().getBindAddress());
        obj.setExternalAddress(config.getNetworkConfiguration().getExternalAddress());
        obj.setLocalNetwork(config.getNetworkConfiguration().getNetwork());
        obj.setLocalSubnet(config.getNetworkConfiguration().getSubnet());
        obj.setUseSbc(config.getNetworkConfiguration().isSbc());
        // Configure Controller parameters
        obj.setLocalBindAddress(config.getControllerConfiguration().getAddress());
        // Configure Media parameter
        obj.setHighestPort(config.getMediaConfiguration().getHighPort());
        obj.setLowestPort(config.getMediaConfiguration().getLowPort());
        obj.setRtpTimeout(config.getMediaConfiguration().getTimeout());
        return obj;
    }

}
