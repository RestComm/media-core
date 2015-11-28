/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.media.server.io.network.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import org.apache.log4j.Logger;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyServer {

    private static final Logger LOGGER = Logger.getLogger(NettyServer.class);

    // Netty Core elements
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap serverBootstrap;

    // Server Properties
    private final String address;
    private final int port;
    // private static final int port = 2727;

    // Server State
    private volatile boolean started;
    private ChannelFuture channelFuture;

    public NettyServer(String address, int port) {
        // Netty Core elements
        this.bossGroup = new NioEventLoopGroup(1);
        this.workerGroup = new NioEventLoopGroup();
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100);

        // Server Properties
        this.address = address;
        this.port = port;
        
        // Server State
        this.started = false;
    }

    public void start() {
        if (!started) {
            try {
                this.started = true;
                this.channelFuture = this.serverBootstrap.bind(this.address, this.port).sync();
            } catch (InterruptedException e) {
                this.started = false;
                LOGGER.error("Could not start Netty Server. Aborting startup.", e);
            }
        }
    }

    public void stop() {
        if (started) {
            this.started = false;
            if (this.channelFuture != null) {
                try {
                    // Wait until the server socket is closed.
                    this.channelFuture.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    LOGGER.warn("Server shutdown was interrupted", e);
                } finally {
                    // Shut down all event loops to terminate all threads.
                    this.bossGroup.shutdownGracefully();
                    this.workerGroup.shutdownGracefully();
                }
            }
        }
    }

}
