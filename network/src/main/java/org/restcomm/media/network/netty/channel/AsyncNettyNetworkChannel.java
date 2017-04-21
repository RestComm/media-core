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

import java.net.SocketAddress;

import org.restcomm.media.network.api.AsynchronousNetworkChannel;
import org.restcomm.media.network.netty.NettyNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.DefaultAddressedEnvelope;

/**
 * Asynchronous network channel powered by Netty.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsyncNettyNetworkChannel<M> implements AsynchronousNetworkChannel<M> {

    public static final int N_THREADS = 1;

    private final NettyNetworkChannelGlobalContext context;
    private final NettyNetworkChannelFsm fsm;

    public AsyncNettyNetworkChannel(NettyNetworkManager networkManager) {
        this(new NettyNetworkChannelGlobalContext(networkManager));
    }

    public AsyncNettyNetworkChannel(NettyNetworkChannelGlobalContext context) {
        this.context = context;
        this.fsm = NettyNetworkChannelFsmBuilder.INSTANCE.build(this.context);
        this.fsm.start();
    }

    @Override
    public boolean isOpen() {
        final NettyNetworkChannelState state = this.fsm.getCurrentState();
        switch (state) {
            case UNINITIALIZED:
            case OPENING:
            case CLOSING:
            case CLOSED:
                return false;

            default:
                return true;
        }
    }

    @Override
    public boolean isBound() {
        final NettyNetworkChannelState state = this.fsm.getCurrentState();
        switch (state) {
            case BOUND:
            case CONNECTING:
            case CONNECTED:
                return true;

            default:
                return false;
        }
    }
    
    @Override
    public SocketAddress getLocalAddress() {
        return isBound() ? this.context.getLocalAddress() : null;
    }

    @Override
    public boolean isConnected() {
        return NettyNetworkChannelState.CONNECTED.equals(this.fsm.getCurrentState());
    }
    
    @Override
    public SocketAddress getRemoteAddress() {
        return isConnected() ? this.context.getRemoteAddress() : null;
    }

    @Override
    public void open(FutureCallback<Void> callback) {
        if (isOpen()) {
            // TODO handle inside FSM Listener
            callback.onFailure(new IllegalStateException("Channel is already open."));
        } else {
            NettyNetworkChannelTransitionContext transitionContext = new NettyNetworkChannelTransitionContext().setCallback(callback);
            this.fsm.fire(NettyNetworkChannelEvent.OPEN, transitionContext);
        }
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        if (isOpen()) {
            NettyNetworkChannelTransitionContext transitionContext = new NettyNetworkChannelTransitionContext().setCallback(callback);
            this.fsm.fire(NettyNetworkChannelEvent.CLOSE, transitionContext);
        } else {
            // TODO handle inside FSM Listener
            callback.onFailure(new IllegalStateException("Channel is already closed."));
        }
    }

    @Override
    public void bind(SocketAddress localAddress, FutureCallback<Void> callback) {
        if (isBound()) {
            // TODO handle inside FSM Listener
            callback.onFailure(new IllegalStateException("Channel is already bound."));
        } else {
            this.context.setLocalAddress(localAddress);
            NettyNetworkChannelTransitionContext transitionContext = new NettyNetworkChannelTransitionContext().setCallback(callback);
            this.fsm.fire(NettyNetworkChannelEvent.BIND, transitionContext);
        }
    }

    @Override
    public void connect(SocketAddress remoteAddress, FutureCallback<Void> callback) {
        if (isConnected()) {
            // TODO handle inside FSM Listener
            callback.onFailure(new IllegalStateException("Channel is already bound."));
        } else {
            this.context.setRemoteAddress(remoteAddress);
            NettyNetworkChannelTransitionContext transitionContext = new NettyNetworkChannelTransitionContext().setCallback(callback);
            this.fsm.fire(NettyNetworkChannelEvent.CONNECT, transitionContext);
        }
    }

    @Override
    public void disconnect(FutureCallback<Void> callback) {
        if (isConnected()) {
            NettyNetworkChannelTransitionContext transitionContext = new NettyNetworkChannelTransitionContext().setCallback(callback);
            this.fsm.fire(NettyNetworkChannelEvent.DISCONNECT, transitionContext);
        } else {
            // TODO handle inside FSM Listener
            callback.onFailure(new IllegalStateException("Channel is not connected."));
        }
    }

    @Override
    public void receive() {
        if (isBound()) {
            this.context.getChannel().read();
        }
    }

    @Override
    public void send(M message, FutureCallback<Void> callback) {
        if (isConnected()) {
            final ChannelFuture future = this.context.getChannel().writeAndFlush(message);
            future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
        } else {
            callback.onFailure(new IllegalStateException("Channel is not connected."));
        }
    }

    @Override
    public void send(M message, SocketAddress remoteAddress, FutureCallback<Void> callback) {
        if (isBound()) {
            if (isConnected()) {
                callback.onFailure(new IllegalStateException("Channel is connected. Cannot send traffic to another peer."));
            } else {
                DefaultAddressedEnvelope<M, SocketAddress> envelope = new DefaultAddressedEnvelope<>(message, remoteAddress);
                final ChannelFuture future = this.context.getChannel().writeAndFlush(envelope);
                future.addListener(new NettyNetworkChannelVoidCallbackListener(callback));
            }
        } else {
            callback.onFailure(new IllegalStateException("Channel is not bound."));
        }
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

}
