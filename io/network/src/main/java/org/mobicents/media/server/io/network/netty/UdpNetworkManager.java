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

package org.mobicents.media.server.io.network.netty;

import java.util.concurrent.atomic.AtomicBoolean;

import org.mobicents.media.server.io.network.PortManager;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Netty-based network manager that provides channels for media and control streaming.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class UdpNetworkManager implements NetworkManager {

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors() * 2;

    private final String address;
    private final PortManager ports;
    private EventLoopGroup eventGroup;
    private final AtomicBoolean active;

    public UdpNetworkManager(String address, PortManager ports) {
        this.address = address;
        this.ports = ports;
        this.active = new AtomicBoolean(false);
    }

    @Override
    public ChannelFuture bindChannel(ChannelHandler handler) throws IllegalStateException {
        return bindChannel(this.address, this.ports.next(), handler);
    }

    @Override
    public ChannelFuture bindChannel(String address, int port, ChannelHandler handler) {
        if (this.active.get()) {
            Bootstrap bootstrap = new Bootstrap().group(this.eventGroup).channel(NioDatagramChannel.class).handler(handler);
            return bootstrap.bind(address, port);
        } else {
            throw new IllegalStateException("Network manager is not active.");
        }
    }

    @Override
    public void activate() throws IllegalStateException {
        if (this.active.get()) {
            throw new IllegalStateException("Network Manager is already active");
        } else {
            this.eventGroup = new NioEventLoopGroup(N_THREADS, new DefaultThreadFactory("netty-client-"));
            this.active.set(true);
        }
    }

    @Override
    public void deactivate() throws IllegalStateException {
        if (this.active.get()) {
            this.eventGroup.shutdownGracefully();
            this.active.set(false);
        } else {
            throw new IllegalStateException("Network Manager is already inactive");
        }
    }

    @Override
    public boolean isActive() {
        return this.active.get();
    }

}
