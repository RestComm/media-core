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

package org.restcomm.media.control.mgcp.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpMessageSubject;

import com.google.common.collect.Sets;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpNettyChannel implements MgcpMessageObserver, MgcpMessageSubject {

    private static final Logger log = Logger.getLogger(MgcpNettyChannel.class);

    public static final int N_THREADS = 1;

    // Network Components
    private EventLoopGroup eventGroup;
    private Channel channel;

    // Handlers
    private final MgcpChannelInitializer initializer;
    private final MgcpInboundHandler inboundHandler;
    private final MgcpMessageDecoder decoder;
    private final MgcpMessageEncoder encoder;

    // Channel State
    private final AtomicBoolean active;

    // Listeners
    private final Set<MgcpMessageObserver> observers;

    public MgcpNettyChannel(MgcpInboundHandler handler, MgcpMessageDecoder decoder, MgcpMessageEncoder encoder) {
        // Handlers
        this.inboundHandler = handler;
        this.decoder = decoder;
        this.encoder = encoder;
        this.initializer = new MgcpChannelInitializer(this.decoder, this.encoder, this.inboundHandler);

        // Channel State
        this.active = new AtomicBoolean(false);

        // Listeners
        this.observers = Sets.newConcurrentHashSet();
    }

    public void open() {
        if (this.active.compareAndSet(false, true)) {
            this.eventGroup = new NioEventLoopGroup(N_THREADS, new DefaultThreadFactory("mgcp-"));
            this.inboundHandler.observe(this);
        } else {
            throw new IllegalArgumentException("Channel is already opened.");
        }
    }

    public void close() {
        if (this.active.compareAndSet(true, false)) {
            // Close channel (if necessary)
            if (channel != null) {
                this.inboundHandler.forget(this);
                ChannelFuture closeFuture = this.channel.close();
                closeFuture.addListener(new ChannelCloseListener());
            }

            // Shutdown event loop
            this.eventGroup.shutdownGracefully(0L, 5L, TimeUnit.SECONDS);
            this.eventGroup = null;
        } else {
            throw new IllegalArgumentException("Channel is already closed.");
        }
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.observers.add(observer);
        if (log.isTraceEnabled()) {
            log.trace("Registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.observers.remove(observer);
        if (log.isTraceEnabled()) {
            log.trace("Unregistered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message,
            MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = (MgcpMessageObserver) iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

    @Override
    public void onMessage(InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        // Forward event
        notify(this, from, to, message, direction);
    }

    public void bind(InetSocketAddress address) {
        Bootstrap bootstrap = new Bootstrap().group(this.eventGroup).channel(NioDatagramChannel.class).handler(this.initializer);
        ChannelFuture bindFuture = bootstrap.bind(address);
        bindFuture.addListener(new ChannelBindListener());
    }

    public void send(MgcpMessage message) throws IOException {
        channel.writeAndFlush(message);
    }

    private final class ChannelBindListener implements ChannelFutureListener {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                MgcpNettyChannel.this.channel = future.channel();
                if (log.isInfoEnabled()) {
                    log.info("Channel was bound to " + MgcpNettyChannel.this.channel.localAddress().toString());
                }
            } else {
                log.error("Could not bind channel.", future.cause());
            }

        }

    }

    private final class ChannelCloseListener implements ChannelFutureListener {

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            final Channel channel = future.channel();

            if (future.isSuccess()) {
                if (log.isInfoEnabled()) {
                    log.info("Channel " + channel.localAddress().toString() + " was closed");
                }
            } else {
                log.error("Could not close channel " + channel.localAddress().toString(), future.cause());
            }

        }

    }

}
