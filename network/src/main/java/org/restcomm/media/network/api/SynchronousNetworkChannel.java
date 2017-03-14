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

package org.restcomm.media.network.api;

import java.io.IOException;
import java.net.SocketAddress;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface SynchronousNetworkChannel<M> {

    boolean isOpen();

    void open() throws IOException;

    void close() throws IOException;

    boolean isBound();

    void bind(SocketAddress localAddress) throws IOException;

    boolean isConnected();

    void connect(SocketAddress remoteAddress) throws IOException;

    void receive() throws IOException;

    void send(M message) throws IOException;

    void send(M message, SocketAddress remoteAddress) throws IOException;

}
