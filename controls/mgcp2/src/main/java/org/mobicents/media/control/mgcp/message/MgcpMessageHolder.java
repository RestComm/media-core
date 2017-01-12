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

package org.mobicents.media.control.mgcp.message;

import java.net.InetSocketAddress;

/**
 * Holds an {@link MgcpMessage} contents while keeping track of the sender and recipient of the message.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMessageHolder {

    private final InetSocketAddress from;
    private final InetSocketAddress to;
    private final MgcpMessage message;

    public MgcpMessageHolder(InetSocketAddress from, InetSocketAddress to, MgcpMessage message) {
        super();
        this.from = from;
        this.to = to;
        this.message = message;
    }

    public InetSocketAddress getFrom() {
        return from;
    }

    public InetSocketAddress getTo() {
        return to;
    }

    public MgcpMessage getMessage() {
        return message;
    }

}
