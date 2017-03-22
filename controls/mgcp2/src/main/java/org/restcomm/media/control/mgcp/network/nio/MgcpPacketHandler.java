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

package org.restcomm.media.control.mgcp.network.nio;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.message.MgcpMessageSubject;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.network.deprecated.channel.PacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;

import com.google.common.collect.Sets;

/**
 * Handler to decode and process incoming MGCP packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @deprecated Use <code>org.restcomm.media.control.mgcp.network.netty</code> package
 */
@Deprecated
public class MgcpPacketHandler implements PacketHandler, MgcpMessageSubject {
    
    private static final Logger log = Logger.getLogger(MgcpPacketHandler.class);

    private final MgcpMessageParser parser;
    private final Set<MgcpMessageObserver> observers;

    public MgcpPacketHandler(MgcpMessageParser parser) {
        this.parser = parser;
        this.observers = Sets.newConcurrentHashSet();
    }

    @Override
    public int compareTo(PacketHandler o) {
        if (o == null) {
            return 1;
        }
        return this.getPipelinePriority() - o.getPipelinePriority();
    }

    @Override
    public boolean canHandle(byte[] packet) {
        return canHandle(packet, packet.length, 0);
    }

    @Override
    public boolean canHandle(byte[] packet, int dataLength, int offset) {
        // TODO [MgcpPacketHandler] Check if packet can be handled somehow
        return true;
    }

    @Override
    public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer)
            throws PacketHandlerException {
        return handle(packet, packet.length, 0, localPeer, remotePeer);
    }

    @Override
    public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer)
            throws PacketHandlerException {
        // Get message type based on first byte
        byte b = packet[0];

        // Produce message according to type
        MgcpMessage message;
        if (b >= 48 && b <= 57) {
            message = handleResponse(packet, dataLength, offset, localPeer, remotePeer);
        } else {
            message = handleRequest(packet, dataLength, offset, localPeer, remotePeer);
        }

        if (log.isDebugEnabled()) {
            log.debug("Incoming MGCP message from " + remotePeer.toString() + ":\n\n" + message.toString() + "\n");
        }

        // Warn listener packet was decoded
        notify(this, remotePeer, localPeer, message, MessageDirection.INCOMING);
        return null;
    }

    private MgcpRequest handleRequest(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer,
            InetSocketAddress remotePeer) throws PacketHandlerException {
        try {
            return this.parser.parseRequest(packet, offset, dataLength);
        } catch (MgcpParseException e) {
            throw new PacketHandlerException(e);
        }
    }

    private MgcpResponse handleResponse(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer,
            InetSocketAddress remotePeer) throws PacketHandlerException {
        try {
            return this.parser.parseResponse(packet, offset, dataLength);
        } catch (MgcpParseException e) {
            throw new PacketHandlerException(e);
        }
    }

    @Override
    public int getPipelinePriority() {
        return 0;
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = (MgcpMessageObserver) iterator.next();
            if(observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

}
