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

package org.restcomm.media.core.control.mgcp.network.netty;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.core.control.mgcp.message.MessageDirection;
import org.restcomm.media.core.control.mgcp.message.MgcpMessage;
import org.restcomm.media.core.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.core.control.mgcp.message.MgcpMessageSubject;

import com.google.common.collect.Sets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Inbound handler that receives incoming MGCP messages and injects them in the MGCP stack for processing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannelInboundHandler extends SimpleChannelInboundHandler<MgcpMessageEnvelope> implements MgcpMessageSubject {

    private static final Logger log = LogManager.getLogger(MgcpChannelInboundHandler.class);

    private final Set<MgcpMessageObserver> observers;

    public MgcpChannelInboundHandler() {
        this.observers = Sets.newConcurrentHashSet();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MgcpMessageEnvelope msg) throws Exception {
        notify(this, (InetSocketAddress) msg.sender(), (InetSocketAddress) msg.recipient(), msg.content(), MessageDirection.INCOMING);
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
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = (MgcpMessageObserver) iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

}
