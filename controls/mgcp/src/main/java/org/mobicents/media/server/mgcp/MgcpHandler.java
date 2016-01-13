/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;

/**
 * Decodes and processes incoming MGCP packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpHandler implements PacketHandler {

    private static final Logger LOGGER = Logger.getLogger(MgcpHandler.class);

    private final MgcpProvider provider;
    private final ByteBuffer rxBuffer;

    public MgcpHandler(MgcpProvider provider) {
        this.provider = provider;
        this.rxBuffer = ByteBuffer.allocateDirect(8192);
    }

    @Override
    public int compareTo(PacketHandler o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean canHandle(byte[] packet) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean canHandle(byte[] packet, int dataLength, int offset) {
        // TODO Auto-generated method stub
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
        // read data into buffer
        this.rxBuffer.clear();
        this.rxBuffer.put(packet, offset, dataLength);
        this.rxBuffer.flip();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received  message with " + dataLength + " bytes length");
        }

        if (this.rxBuffer.limit() > 0) {
            byte b = this.rxBuffer.get();
            this.rxBuffer.rewind();

            // update event ID
            int msgType = (b >= 48 && b <= 57) ? MgcpEvent.RESPONSE : MgcpEvent.REQUEST;
            MgcpEvent evt = this.provider.createEvent(msgType, remotePeer);

            // parse message
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Parsing message: " + new String(packet, 0, dataLength));
            }
            evt.getMessage().read(this.rxBuffer);

            // Tell provider about incoming event
            this.provider.onMgcpEventReceived(evt);
        }

        return null;
    }

    @Override
    public int getPipelinePriority() {
        // TODO Auto-generated method stub
        return 0;
    }

}
