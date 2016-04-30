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

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.MgcpMessage;
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.io.network.channel.PacketHandler;

/**
 * UDP channel that handles MGCP traffic.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannel extends MultiplexedChannel implements MgcpMessageListener {

    private static final Logger log = Logger.getLogger(MgcpChannel.class);

    // Core Components
    private final UdpManager networkManager;

    // MGCP Channel
    private final PacketHandler mgcpHandler;
    private final SocketAddress bindAddress;
    private boolean open;

    public MgcpChannel(SocketAddress bindAddress, UdpManager networkManager, MgcpPacketHandler packetHandler) {
        // Core Components
        this.networkManager = networkManager;

        // MGCP Channel
        this.bindAddress = bindAddress;
        this.mgcpHandler = packetHandler;
        this.handlers.addHandler(mgcpHandler);
        this.open = false;
    }

    @Override
    public void open() throws IllegalStateException, IOException {
        if (this.open) {
            throw new IllegalStateException("MGCP channel is already open.");
        } else {
            // Open channel
            this.selectionKey = this.networkManager.open(this);
            this.dataChannel = (DatagramChannel) this.selectionKey.channel();

            // Bind channel
            try {
                this.dataChannel.bind(this.bindAddress);
            } catch (IOException e) {
                log.error("BOMBED!!");
                close();
                throw e;
            }

            // Declare the channel officially active
            this.open = true;
            if (log.isInfoEnabled()) {
                log.info("MGCP Channel is open on " + this.bindAddress.toString());
            }
        }
    }

    @Override
    public void close() throws IllegalStateException {
        if (this.open) {
            // Close the channel
            super.close();

            // Declare channel officially inactive
            this.open = false;
            if (log.isInfoEnabled()) {
                log.info("MGCP Channel is closed");
            }
        } else {
            throw new IllegalStateException("MGCP channel is already closed.");
        }
    }

    @Override
    public boolean isOpen() {
        return this.open;
    }

    public void queue(byte[] data) {
        if (this.open) {
            queueData(data);
        } else {
            throw new IllegalStateException("Channel is closed");
        }
    }

    @Override
    public void onMgcpMessage(MgcpMessage request) {
        // TODO Auto-generated method stub

    }

}
