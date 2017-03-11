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

package org.restcomm.media.network.netty;

import java.io.IOException;
import java.net.SocketAddress;

import org.restcomm.media.network.api.AsyncNetworkChannel;
import org.restcomm.media.network.api.NetworkChannel;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;

/**
 * Network channel powered by Netty that features both a Sync and Async API.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannel<M> implements AsyncNetworkChannel<M>, NetworkChannel<M> {

    public static final int N_THREADS = 1;

    // Netty Components
    private EventLoopGroup eventGroup;
    private Bootstrap bootstrap;
    private Channel channel;

    public NettyNetworkChannel() {
        super();
    }
    
    /*
     * SYNC API
     */
    @Override
    public void open() {
        this.eventGroup = new NioEventLoopGroup(N_THREADS);
        this.bootstrap  = new Bootstrap().group(this.eventGroup).channel(NioDatagramChannel.class);
    }
    
    @Override
    public void close() throws IOException {
        try {
            this.channel.close().sync();
        } catch (Exception e) {
            throw new IOException("Could not close the channel.", e);
        }
    }

    /*
     * ASYNC API
     */
    @Override
    public void open(FutureCallback<Void> callback) {
        this.eventGroup = new NioEventLoopGroup(N_THREADS);
        this.bootstrap  = new Bootstrap().group(this.eventGroup).channel(NioDatagramChannel.class);
        callback.onSuccess(null);
        // TODO check what open does
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.close();
        future.addListener(new ChannelCallbackListener(callback));
    }

    @Override
    public void bind(SocketAddress localAddress, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.bind(localAddress);
        future.addListener(new ChannelCallbackListener(callback));
    }

    @Override
    public void connect(SocketAddress remoteAddress, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.connect(remoteAddress);
        future.addListener(new ChannelCallbackListener(callback));
    }

    @Override
    public void receive() {
        this.channel.read();
    }

    @Override
    public void send(M message, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.writeAndFlush(message);
        future.addListener(new ChannelCallbackListener(callback));
    }

    @Override
    public void send(M message, SocketAddress remoteAddress, FutureCallback<Void> callback) {
        // Wrap the message in an envelop
        DefaultAddressedEnvelope<M, SocketAddress> envelope = new DefaultAddressedEnvelope<>(message, remoteAddress);
        ChannelFuture future = this.channel.writeAndFlush(envelope);
        future.addListener(new ChannelCallbackListener(callback));
    }

    private static final class ChannelCallbackListener implements ChannelFutureListener {

        private final FutureCallback<Void> observer;

        public ChannelCallbackListener(FutureCallback<Void> observer) {
            super();
            this.observer = observer;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                this.observer.onSuccess(null);
            } else {
                this.observer.onFailure(future.cause());
            }

        }
    }

}
