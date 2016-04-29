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

package org.mobicents.media.control.mgcp.io;

import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.MgcpRequest;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpPacketHandler implements PacketHandler {

    private static final Logger log = Logger.getLogger(MgcpPacketHandler.class);

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
        // TODO [MgcpPacketHandler] Check if packet can be handled
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
        if (log.isDebugEnabled()) {
            log.debug("Parsing message: " + new String(packet, offset, dataLength));
        }

        // Get message type based on first byte
        byte b = packet[0];
        if (b >= 48 && b <= 57) {
            handleResponse(packet, dataLength, offset, localPeer, remotePeer);
        } else {
            handleRequest(packet, dataLength, offset, localPeer, remotePeer);
        }
        return null;
    }

    private void handleRequest(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer,
            InetSocketAddress remotePeer) {
        MgcpRequest mgcpRequest = new MgcpRequest();
        
    }

    private void handleResponse(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer,
            InetSocketAddress remotePeer) {
        // TODO implement handleResponse
    }

    @Override
    public int getPipelinePriority() {
        return 0;
    }

}
