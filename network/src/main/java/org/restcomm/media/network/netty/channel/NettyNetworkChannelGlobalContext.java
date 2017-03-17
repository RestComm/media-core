/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.network.netty.channel;

import java.net.SocketAddress;

import org.restcomm.media.network.api.AsynchronousNetworkManager;

import io.netty.channel.Channel;

/**
 * The runtime context of a {@link AsyncNettyNetworkChannel}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannelGlobalContext {

    private AsynchronousNetworkManager<Channel> networkManager;
    private Channel channel;
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;

    public NettyNetworkChannelGlobalContext(AsynchronousNetworkManager<Channel> networkManager) {
        super();
        this.networkManager = networkManager;
    }

    AsynchronousNetworkManager<Channel> getNetworkManager() {
        return networkManager;
    }

    Channel getChannel() {
        return channel;
    }

    void setChannel(Channel channel) {
        this.channel = channel;
    }

    SocketAddress getLocalAddress() {
        return localAddress;
    }

    void setLocalAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    SocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    void setRemoteAddress(SocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    void clean() {
        this.channel = null;
        this.localAddress = null;
        this.remoteAddress = null;
        this.networkManager = null;
    }

}
