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

package org.restcomm.media.bootstrap.ioc.provider.mgcp;

import org.restcomm.media.control.mgcp.network.MgcpChannel;
import org.restcomm.media.control.mgcp.network.MgcpPacketHandler;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.network.deprecated.channel.NetworkGuard;
import org.restcomm.media.network.deprecated.channel.RestrictedNetworkGuard;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannelProvider implements Provider<MgcpChannel> {

    private final MgcpPacketHandler mgcpHandler;
    private final NetworkGuard networkGuard;

    @Inject
    public MgcpChannelProvider(UdpManager networkManager, MgcpPacketHandler mgcpHandler) {
        this.networkGuard = new RestrictedNetworkGuard(networkManager.getLocalBindAddress(), networkManager.getLocalSubnet());
        this.mgcpHandler = mgcpHandler;
    }

    @Override
    public MgcpChannel get() {
        return new MgcpChannel(this.networkGuard, this.mgcpHandler);
    }

}
