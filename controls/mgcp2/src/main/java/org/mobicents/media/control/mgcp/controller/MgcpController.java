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

package org.mobicents.media.control.mgcp.controller;

import java.io.IOException;
import java.net.SocketAddress;

import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.network.MgcpChannel;
import org.mobicents.media.control.mgcp.network.MgcpPacketHandler;
import org.mobicents.media.control.mgcp.transaction.MgcpTransactionManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.spi.ControlProtocol;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.ServerManager;

import com.google.inject.Inject;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpController implements ServerManager, MgcpMessageListener {

    // MGCP Components
    private final MgcpMessageParser messageParser;
    private final MgcpPacketHandler packetHandler;
    private final MgcpTransactionManager transactions;
    private final MgcpChannel channel;

    // MGCP Controller State
    private boolean active;

    public MgcpController(SocketAddress bindAddress, int minTransactionId, int maxTransactionId, UdpManager networkManager, MgcpCommandProvider commandProvider) {
        // MGCP Components
        this.messageParser = new MgcpMessageParser();
        this.packetHandler = new MgcpPacketHandler(this.messageParser, this);
        this.channel = new MgcpChannel(bindAddress, networkManager, packetHandler);
        this.transactions = new MgcpTransactionManager(minTransactionId, maxTransactionId, this.channel, commandProvider);

        // MGCP Controller State
        this.active = false;
    }

    @Override
    public ControlProtocol getControlProtocol() {
        return ControlProtocol.MGPC;
    }

    @Override
    public void activate() throws IllegalStateException {
        if (this.active) {
            throw new IllegalStateException("Controller is already active");
        } else {
            // TODO start resources
            try {
                channel.open();
                this.active = true;
            } catch (IOException e) {
                // TODO throw exception
            }
        }
    }

    @Override
    public void deactivate() throws IllegalStateException {
        if (this.active) {
            // TODO stop resources
            this.active = false;
        } else {
            throw new IllegalStateException("Controller is already inactive");
        }
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    @Override
    public void onStarted(Endpoint endpoint, EndpointInstaller installer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopped(Endpoint endpoint) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMessageReceived(MgcpMessage message) {
        // Ask the transaction manager to process the incoming message
        // If message is a Request, then a new transaction is spawned and executed.
        // If message is a Response, then existing transaction is retrieved and closed.
        this.transactions.process(message, MessageDirection.INBOUND);
    }

}
