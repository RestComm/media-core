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

package org.restcomm.media.bootstrap.ioc.provider.mgcp;

import org.restcomm.media.control.mgcp.command.MgcpCommandProvider;
import org.restcomm.media.control.mgcp.controller.MgcpController;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.network.netty.AsyncMgcpChannel;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionManager;
import org.restcomm.media.core.configuration.MediaServerConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Mgcp2ControllerProvider implements Provider<MgcpController> {

    private final String address;
    private final int port;
    private final AsyncMgcpChannel channel;
    private final MgcpTransactionManager transactions;
    private final MgcpEndpointManager endpoints;
    private final MgcpCommandProvider commands;

    @Inject
    public Mgcp2ControllerProvider(MediaServerConfiguration config, AsyncMgcpChannel channel, MgcpTransactionManager transactions, MgcpEndpointManager endpoints, MgcpCommandProvider commands) {
        this.address = config.getControllerConfiguration().getAddress();
        this.port = config.getControllerConfiguration().getPort();
        this.channel = channel;
        this.transactions = transactions;
        this.endpoints = endpoints;
        this.commands = commands;
    }

    @Override
    public MgcpController get() {
        return new MgcpController(this.address, this.port, this.channel, this.transactions, this.endpoints, this.commands);
    }

}
