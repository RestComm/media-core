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

import org.mobicents.media.control.mgcp.message.MgcpMessageSubject;
import org.mobicents.media.control.mgcp.network.MgcpChannel;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.spi.ControlProtocol;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.ServerManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpController implements ServerManager {

    // MGCP Components
    private final MgcpMessageSubject processor;
    private final MgcpChannel channel;

    // MGCP Controller State
    private boolean active;

    public MgcpController(SocketAddress bindAddress, UdpManager networkManager, MgcpMessageSubject messageCenter) {
        // MGCP Components
        this.processor = messageCenter;
        this.channel = new MgcpChannel(bindAddress, networkManager, this.processor);

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
            try {
                this.channel.open();
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
            this.channel.close();
            // TODO clear transactions
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

}
