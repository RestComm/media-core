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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.network.deprecated.netty.NetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Implementation if {@link NettyNetworkChannelFsm} that uses Asynchronous API of {@link NetworkManager}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsyncNettyNetworkChannelFsm extends AbstractNettyNetworkChannelFsm {

    private static final Logger log = LogManager.getLogger(AsyncNettyNetworkChannelFsm.class);
    
    private final NettyNetworkChannelGlobalContext globalContext;
    
    public AsyncNettyNetworkChannelFsm(NettyNetworkChannelGlobalContext globalContext) {
        super();
        this.globalContext = globalContext;
    }

    @Override
    public void enterOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        // Try opening the channel
        // A listener will get the response asynchronously
        this.globalContext.getNetworkManager().openChannel(new OpenCallback(context));
    }

    @Override
    public void enterBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        // Bind channel to local address
        // A listener will get the response asynchronously
        final ChannelFuture future = this.globalContext.getChannel().bind(this.globalContext.getLocalAddress());
        future.addListener(new BindCallback(context));
    }

    @Override
    public void enterConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        final ChannelFuture future = this.globalContext.getChannel().connect(this.globalContext.getRemoteAddress());
        future.addListener(new ConnectCallback(context));
    }

    @Override
    public void enterDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        // Disconnect channel from remote peer
        // A listener will get the response asynchronously
        final ChannelFuture future = this.globalContext.getChannel().disconnect();
        future.addListener(new DisconnectCallback(context));
    }

    @Override
    public void exitDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        // Clean reference to remote address
        this.globalContext.setRemoteAddress(null);
    }

    @Override
    public void enterClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        final ChannelFuture future = this.globalContext.getChannel().close();
        future.addListener(new CloseCallback(context));
    }

    @Override
    public void enterClosed(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelTransitionContext context) {
        this.globalContext.clean();
    }

    private final class OpenCallback implements FutureCallback<Channel> {

        private final NettyNetworkChannelTransitionContext context;

        public OpenCallback(NettyNetworkChannelTransitionContext context) {
            this.context = context;
        }

        @Override
        public void onSuccess(Channel result) {
            if(log.isTraceEnabled()) {
                log.trace("Channel opened successfully");
            }
            globalContext.setChannel(result);
            fire(NettyNetworkChannelEvent.OPENED, this.context);
            this.context.getCallback().onSuccess(null);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Channel could not be opened, so will be terminated prematurely.", t);
            fire(NettyNetworkChannelEvent.CLOSE, this.context);
            this.context.getCallback().onFailure(t);
        }

    }

    private final class BindCallback implements ChannelFutureListener {

        private final NettyNetworkChannelTransitionContext context;

        public BindCallback(NettyNetworkChannelTransitionContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel is bound to " + future.channel().localAddress().toString());
                }
                fire(NettyNetworkChannelEvent.BOUND, this.context);
                this.context.getCallback().onSuccess(null);
            } else {
                log.warn("Could not bind channel to " + globalContext.getLocalAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
                this.context.getCallback().onFailure(future.cause());
            }
        }

    }

    private final class ConnectCallback implements ChannelFutureListener {

        private final NettyNetworkChannelTransitionContext context;

        public ConnectCallback(NettyNetworkChannelTransitionContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel is connected to " + future.channel().remoteAddress().toString());
                }
                fire(NettyNetworkChannelEvent.CONNECTED, this.context);
                this.context.getCallback().onSuccess(null);
            } else {
                log.warn("Could not connect channel to " + globalContext.getRemoteAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
                this.context.getCallback().onFailure(future.cause());
            }
        }

    }

    private final class DisconnectCallback implements ChannelFutureListener {

        private final NettyNetworkChannelTransitionContext context;

        public DisconnectCallback(NettyNetworkChannelTransitionContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel disconnected from " + future.channel().remoteAddress().toString());
                }
                fire(NettyNetworkChannelEvent.DISCONNECTED, this.context);
                this.context.getCallback().onSuccess(null);
            } else {
                log.warn("Could not disconnect channel from " + globalContext.getRemoteAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
                this.context.getCallback().onFailure(future.cause());
            }
        }

    }

    private final class CloseCallback implements ChannelFutureListener {
        
        private final NettyNetworkChannelTransitionContext context;
        
        public CloseCallback(NettyNetworkChannelTransitionContext context) {
            this.context = context;
        }
        
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel closed in elegant manner");
                }
                this.context.getCallback().onSuccess(null);
            } else {
                log.warn("Channel could not be closed properly", future.cause());
                this.context.getCallback().onFailure(future.cause());
            }
            fire(NettyNetworkChannelEvent.CLOSED, this.context);
        }
        
    }

}
