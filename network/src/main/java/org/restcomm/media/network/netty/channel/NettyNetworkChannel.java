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

package org.restcomm.media.network.netty.channel;

import java.io.IOException;
import java.net.SocketAddress;

import org.restcomm.media.network.api.NetworkChannel;
import org.restcomm.media.network.netty.NettyNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultAddressedEnvelope;

/**
 * Network channel powered by Netty that features both a Sync and Async API.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannel<M> implements NetworkChannel<M> {

    public static final int N_THREADS = 1;

    // Netty Components
    private NettyNetworkManager networkManager;
    private Channel channel;

    public NettyNetworkChannel(NettyNetworkManager networkManager) {
        this.networkManager = networkManager;
    }
    
    /*
     * COMMON API
     */
    @Override
    public boolean isOpen() {
        return (this.channel != null && channel.isOpen());
    }
    
    @Override
    public boolean isBound() {
        return (this.channel != null && channel.localAddress() != null);
    }
    
    @Override
    public boolean isConnected() {
        return (this.channel != null && channel.remoteAddress() != null);
    }
    
    @Override
    public void receive() {
        this.channel.read();
    }

    /*
     * SYNC API
     */
    @Override
    public void open() throws IOException {
        this.channel = this.networkManager.openChannel();
    }

    @Override
    public void close() throws IOException {
        try {
            this.channel.close().sync();
        } catch (Exception e) {
            throw new IOException("Could not close the channel.", e);
        }
    }

    @Override
    public void bind(SocketAddress localAddress) throws IOException {
        try {
            this.channel.bind(localAddress).sync();
        } catch (Exception e) {
            throw new IOException("Could not bind channel to address " + localAddress.toString(), e);
        }
    }

    @Override
    public void connect(SocketAddress remoteAddress) throws IOException {
        try {
            this.channel.connect(remoteAddress).sync();
        } catch (Exception e) {
            throw new IOException("Could not connect channel " + this.channel.localAddress().toString() + " to " + remoteAddress.toString(), e);
        }
    }

    @Override
    public void send(M message) throws IOException {
        try {
            this.channel.writeAndFlush(message).sync();
        } catch (Exception e) {
            throw new IOException("Could not send message to remote peer " + channel.remoteAddress().toString(), e);
        }
    }

    @Override
    public void send(M message, SocketAddress remoteAddress) throws IOException {
        try {
            // Wrap message in addressed envelope
            AddressedEnvelope<M, SocketAddress> envelope = new DefaultAddressedEnvelope<>(message, remoteAddress);
            // Send message
            this.channel.writeAndFlush(envelope).sync();
        } catch (Exception e) {
            throw new IOException("Could not send message to remote peer " + remoteAddress.toString(), e);
        }
    }

    /*
     * ASYNC API
     */
    @Override
    public void open(FutureCallback<Void> callback) {
        this.networkManager.openChannel(new NettyNetworkChannelCallbackListener(callback));
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.close();
        future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
    }

    @Override
    public void bind(SocketAddress localAddress, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.bind(localAddress);
        future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
    }

    @Override
    public void connect(SocketAddress remoteAddress, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.connect(remoteAddress);
        future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
    }

    @Override
    public void send(M message, FutureCallback<Void> callback) {
        ChannelFuture future = this.channel.writeAndFlush(message);
        future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
    }

    @Override
    public void send(M message, SocketAddress remoteAddress, FutureCallback<Void> callback) {
        // Wrap the message in an envelop
        DefaultAddressedEnvelope<M, SocketAddress> envelope = new DefaultAddressedEnvelope<>(message, remoteAddress);
        ChannelFuture future = this.channel.writeAndFlush(envelope);
        future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
    }

    static final class NettyNetworkChannelVoidCallbackListener implements ChannelFutureListener {

        private final FutureCallback<Void> observer;

        public NettyNetworkChannelVoidCallbackListener(FutureCallback<Void> observer) {
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

    final class NettyNetworkChannelCallbackListener implements FutureCallback<Channel> {

        private final FutureCallback<Void> observer;

        public NettyNetworkChannelCallbackListener(FutureCallback<Void> observer) {
            this.observer = observer;
        }

        @Override
        public void onSuccess(Channel result) {
            NettyNetworkChannel.this.channel = result;
            observer.onSuccess(null);
        }

        @Override
        public void onFailure(Throwable t) {
            observer.onFailure(t);
        }
    }

}
