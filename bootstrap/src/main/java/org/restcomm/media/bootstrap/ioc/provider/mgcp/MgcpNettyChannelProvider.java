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

import org.restcomm.media.control.mgcp.message.MgcpMessageParser;
import org.restcomm.media.control.mgcp.network.MgcpInboundHandler;
import org.restcomm.media.control.mgcp.network.MgcpMessageDecoder;
import org.restcomm.media.control.mgcp.network.MgcpMessageEncoder;
import org.restcomm.media.control.mgcp.network.MgcpNettyChannel;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpNettyChannelProvider implements Provider<MgcpNettyChannel> {


    private final MgcpMessageEncoder encoder;
    private final MgcpMessageDecoder decoder;
    private final MgcpInboundHandler handler;

    @Inject
    public MgcpNettyChannelProvider(MgcpMessageParser parser) {
        this.encoder = new MgcpMessageEncoder();
        this.decoder = new MgcpMessageDecoder(parser);
        this.handler = new MgcpInboundHandler();
    }

    @Override
    public MgcpNettyChannel get() {
        return new MgcpNettyChannel(handler, decoder, encoder);
    }

}
