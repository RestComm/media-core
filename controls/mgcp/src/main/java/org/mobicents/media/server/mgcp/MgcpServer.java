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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.network.server.NioServer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpServer {

    private final static Logger logger = Logger.getLogger(MgcpServer.class);

    private final NioServer server;
    private final MgcpHandler handler;
    private Selector selector;
    private final AtomicBoolean started;

    public MgcpServer(MgcpHandler handler) {
        this.server = new NioServer();
        this.handler = handler;
        this.started = new AtomicBoolean(false);
        this.server.addPacketHandler(handler);
    }

    public void start() throws IOException {
        if (!this.started.get()) {
            this.started.set(true);
            this.selector = Selector.open();
            this.server.start(this.selector);
        }
    }

    public void stop() throws IOException {
        if (this.started.get()) {
            this.started.set(false);
            this.server.stop();
            this.selector.close();
            this.selector = null;
        }
    }

    public SelectionKey registerChannel(String address, int port) {
        if (this.started.get()) {
            try {
                DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);

                try {
                    channel.bind(new InetSocketAddress(address, port));
                    SelectionKey key = channel.register(this.selector, SelectionKey.OP_READ);
                    key.attach(this.handler);
                    return key;
                } catch (IOException e) {
                    logger.warn("Could not bind datagram channel for MGCP server.", e);
                    channel.close();
                }
            } catch (IOException e) {
                logger.warn("Could not open datagram channel for MGCP server.", e);
            }
        }
        return null;
    }

}
