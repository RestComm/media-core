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

/**
 * Provides and multiplexes a group of {@link SynchronousNetworkChannel}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @param <C> The type of channel produced by the Network Manager.
 */
public interface SynchronousNetworkManager<C> extends AutoCloseable {

    /**
     * Opens and registers a new Network Channel.
     * 
     * @return A new {@link SynchronousNetworkChannel}
     * 
     * @throws IOException If an error occurs when creating the channel.
     */
    public C openChannel() throws IOException;

    /**
     * Shuts down the Network Manager, closing any registered channels.
     * 
     * @throws IOException If any error occurs when closing the manager.
     */
    @Override
    void close() throws IOException;

}
