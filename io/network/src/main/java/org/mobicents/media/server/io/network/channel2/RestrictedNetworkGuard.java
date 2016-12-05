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

package org.mobicents.media.server.io.network.channel2;

import java.net.InetAddress;
import java.net.SocketAddress;

import org.mobicents.media.server.io.network.IPAddressCompare;

/**
 * {@link NetworkGuard} implementation that accepts traffic coming from within the local network only.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestrictedNetworkGuard implements NetworkGuard {

    private final InetAddress address;
    private final InetAddress network;
    private final InetAddress subnet;

    public RestrictedNetworkGuard(InetAddress address, InetAddress network, InetAddress subnet) {
        this.address = address;
        this.network = network;
        this.subnet = subnet;
    }

    @Override
    public boolean isSecure(NetworkChannel channel, SocketAddress source) {
        return IPAddressCompare.isInRangeV4(this.network.getAddress(), this.subnet.getAddress(), address.getAddress());
    }

}
