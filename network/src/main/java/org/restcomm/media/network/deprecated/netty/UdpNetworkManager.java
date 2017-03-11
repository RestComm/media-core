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

package org.restcomm.media.network.deprecated.netty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * Netty-based network manager that provides datagram channels.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class UdpNetworkManager implements NetworkManager {

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors();
    public static final long SHUTDOWN_TIME = 5L;
    
    private EventLoopGroup eventGroup;
    private final AtomicBoolean active;
    
    /**
     * Creates a new Network Manager.
     * 
     * @param address The address the manager will bind channels to.
     * @param portManager The port range manager.
     */
    public UdpNetworkManager() {
        this.active = new AtomicBoolean(false);
    }

    @Override
    public ChannelFuture bindDatagramChannel(String address, int port, ChannelHandler handler) throws IllegalStateException {
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
            this.eventGroup.shutdownGracefully(0L, SHUTDOWN_TIME, TimeUnit.SECONDS);
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
