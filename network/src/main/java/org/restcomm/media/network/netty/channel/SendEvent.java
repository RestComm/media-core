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

package org.restcomm.media.network.netty.channel;

import java.net.SocketAddress;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SendEvent<M> implements NettyNetworkChannelEvent {

    private final SocketAddress recipient;
    private final M message;

    /**
     * Event to send a message to the remote peer. Only usable with connectionless channels.
     * 
     * @param message The message to be sent
     * @param recipient The recipient of the message
     */
    SendEvent(M message, SocketAddress recipient) {
        this.recipient = recipient;
        this.message = message;
    }

    /**
     * Event to send a message to the remote peer. Only usable with connected channels.
     * 
     * @param message The message to be sent
     */
    public SendEvent(M message) {
        this(message, null);
    }

    public SocketAddress getRecipient() {
        return recipient;
    }

    public M getMessage() {
        return message;
    }

}
