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

import org.apache.log4j.Logger;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Implementation 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ConcreteNettyNetworkChannelFsm extends AbstractNettyNetworkChannelFsm {

    private static final Logger log = Logger.getLogger(ConcreteNettyNetworkChannelFsm.class);

    @Override
    public void enterOpening(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        // Try opening the channel
        // A listener will get the response asynchronously
        context.getNetworkManager().openChannel(new OpenCallback(context));
    }

    @Override
    public void enterBinding(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        // Bind channel to local address
        // A listener will get the response asynchronously
        final ChannelFuture future = context.getChannel().bind(context.getLocalAddress());
        future.addListener(new BindCallback(context));
    }

    @Override
    public void enterConnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        final ChannelFuture future = context.getChannel().connect(context.getRemoteAddress());
        future.addListener(new ConnectCallback(context));
    }

    @Override
    public void enterDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        // Disconnect channel from remote peer
        // A listener will get the response asynchronously
        final ChannelFuture future = context.getChannel().disconnect();
        future.addListener(new DisconnectCallback(context));
    }

    @Override
    public void exitDisconnecting(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        // Clean reference to remote address
        context.setRemoteAddress(null);
    }

    @Override
    public void enterClosing(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        final ChannelFuture future = context.getChannel().close();
        future.addListener(new CloseCallback(context));
    }

    @Override
    public void enterClosed(NettyNetworkChannelState from, NettyNetworkChannelState to, NettyNetworkChannelEvent event, NettyNetworkChannelContext context) {
        context.clean();
    }

    private final class OpenCallback implements FutureCallback<Channel> {

        private final NettyNetworkChannelContext context;

        public OpenCallback(NettyNetworkChannelContext context) {
            this.context = context;
        }

        @Override
        public void onSuccess(Channel result) {
            if(log.isTraceEnabled()) {
                log.trace("Channel opened successfully");
            }
            this.context.setChannel(result);
            fire(NettyNetworkChannelEvent.OPENED, this.context);
        }

        @Override
        public void onFailure(Throwable t) {
            log.warn("Channel could not be opened, so will be terminated prematurely.", t);
            fire(NettyNetworkChannelEvent.CLOSE, this.context);
        }

    }

    private final class BindCallback implements ChannelFutureListener {

        private final NettyNetworkChannelContext context;

        public BindCallback(NettyNetworkChannelContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel is bound to " + future.channel().localAddress().toString());
                }
                fire(NettyNetworkChannelEvent.BOUND, this.context);
            } else {
                log.warn("Could not bind channel to " + context.getLocalAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
            }
        }

    }

    private final class ConnectCallback implements ChannelFutureListener {

        private final NettyNetworkChannelContext context;

        public ConnectCallback(NettyNetworkChannelContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel is connected to " + future.channel().remoteAddress().toString());
                }
                fire(NettyNetworkChannelEvent.CONNECTED, this.context);
            } else {
                log.warn("Could not connect channel to " + context.getRemoteAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
            }
        }

    }

    private final class DisconnectCallback implements ChannelFutureListener {

        private final NettyNetworkChannelContext context;

        public DisconnectCallback(NettyNetworkChannelContext context) {
            this.context = context;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel disconnected from " + future.channel().remoteAddress().toString());
                }
                fire(NettyNetworkChannelEvent.DISCONNECTED, this.context);
            } else {
                log.warn("Could not disconnect channel from " + context.getRemoteAddress() + ". Channel will be closed.", future.cause());
                fire(NettyNetworkChannelEvent.CLOSE, this.context);
            }
        }

    }

    private final class CloseCallback implements ChannelFutureListener {
        
        private final NettyNetworkChannelContext context;
        
        public CloseCallback(NettyNetworkChannelContext context) {
            this.context = context;
        }
        
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                if(log.isDebugEnabled()) {
                    log.debug("Channel closed in elegant manner");
                }
            } else {
                log.warn("Channel could not be closed properly", future.cause());
            }
            fire(NettyNetworkChannelEvent.CLOSED, this.context);
        }
        
    }

}
