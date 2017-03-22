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

package org.restcomm.media.network.netty;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.restcomm.media.network.api.AsynchronousNetworkManager;
import org.restcomm.media.network.api.SynchronousNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * Netty-based Network Manager that features both synchronous and asynchronous API.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 * @see SynchronousNetworkManager
 * @see AsynchronousNetworkManager
 *
 */
public class NettyNetworkManager implements SynchronousNetworkManager<Channel>, AsynchronousNetworkManager<Channel> {

    static final long SHUTDOWN_TIMEOUT = 5L;
    static final int N_THREADS = Runtime.getRuntime().availableProcessors();

    protected final EventLoopGroup eventGroup;
    protected final Bootstrap bootstrap;
    private final AtomicBoolean open;

    public NettyNetworkManager() {
        this(N_THREADS);
    }

    public NettyNetworkManager(int threadCount) {
        this(new Bootstrap().channel(NioDatagramChannel.class).group(new NioEventLoopGroup(threadCount)));
    }

    public NettyNetworkManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
        this.eventGroup = bootstrap.group();
        this.open = new AtomicBoolean(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException If manager is already closed.
     */
    @Override
    public Channel openChannel() throws IOException, IllegalStateException {
        assertOpen();
        try {
            return this.bootstrap.clone().register().sync().channel();
        } catch (Exception e) {
            throw new IOException("Could not open channel.", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException If manager is already closed.
     */
    @Override
    public void openChannel(FutureCallback<Channel> callback) throws IllegalStateException {
        assertOpen();

        ChannelFuture future = this.bootstrap.clone().register();
        future.addListener(new NetworkManagerChannelFutureCallback(callback));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException If manager is already closed.
     */
    @Override
    public void close() throws IOException, IllegalStateException {
        if (this.open.compareAndSet(true, false)) {
            try {
                this.eventGroup.shutdownGracefully(0L, 5L, TimeUnit.SECONDS).sync();
            } catch (Exception e) {
                throw new IOException("Could not be gracefully closed.", e);
            }
        } else {
            throw new IllegalStateException("Network Manager is already closed");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalStateException If manager is already closed.
     */
    @Override
    public void close(FutureCallback<Void> callback) throws IllegalStateException {
        assertOpen();

        Future<?> future = this.eventGroup.shutdownGracefully(0L, 5L, TimeUnit.SECONDS);
        future.addListener(new NetworkManagerVoidFutureCallback(callback));
    }

    private void assertOpen() throws IllegalStateException {
        if (!this.open.get()) {
            throw new IllegalStateException("Network Manager is already closed.");
        }
    }

    private static final class NetworkManagerVoidFutureCallback implements GenericFutureListener<Future<Object>> {

        private final FutureCallback<Void> callback;

        public NetworkManagerVoidFutureCallback(FutureCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void operationComplete(Future<Object> future) throws Exception {
            if (future.isSuccess()) {
                this.callback.onSuccess(null);
            } else {
                this.callback.onFailure(future.cause());
            }
        }

    }

    private static final class NetworkManagerChannelFutureCallback implements GenericFutureListener<ChannelFuture> {

        private final FutureCallback<Channel> callback;

        public NetworkManagerChannelFutureCallback(FutureCallback<Channel> callback) {
            this.callback = callback;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                this.callback.onSuccess(future.channel());
            } else {
                this.callback.onFailure(future.cause());
            }
        }

    }

}
