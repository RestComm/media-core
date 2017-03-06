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

package org.restcomm.media.control.mgcp;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpPacketHandler implements PacketHandler {

    private static final Logger log = Logger.getLogger(MgcpPacketHandler.class);
    
    private final MgcpProvider provider;
    
    public MgcpPacketHandler(MgcpProvider provider) {
        this.provider = provider;
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
        // TODO [MGCPHandler] Check if packet can be handled
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
        try {
            // Create event
            byte b = packet[0];
            int msgType = (b >= 48 && b <= 57) ? MgcpEvent.RESPONSE : MgcpEvent.REQUEST;
            MgcpEvent evt = provider.createEvent(msgType, remotePeer);

            // Parse message
            if (log.isDebugEnabled()) {
                log.debug("Parsing message: " + new String(packet, offset, dataLength));
            }
            evt.getMessage().parse(packet, offset, dataLength);

            // Dispatch message to MGCP provider
            this.provider.onIncomingEvent(evt);
        } catch (Exception e) {
            throw new PacketHandlerException(e);
        }
        return null;
    }

    @Override
    public int getPipelinePriority() {
        return 0;
    }

}
