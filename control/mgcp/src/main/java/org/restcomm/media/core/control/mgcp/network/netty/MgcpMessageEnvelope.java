/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.core.control.mgcp.network.netty;

import java.net.SocketAddress;

import org.restcomm.media.core.control.mgcp.message.MgcpMessage;

import io.netty.channel.DefaultAddressedEnvelope;

/**
 * Wraps an {@link MgcpMessage} in an envelope to be sent to a specific recipient.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageEnvelope extends DefaultAddressedEnvelope<MgcpMessage, SocketAddress> {

    public MgcpMessageEnvelope(MgcpMessage message, SocketAddress recipient, SocketAddress sender) {
        super(message, recipient, sender);
    }

}
