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

package org.restcomm.media.network.deprecated.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;

/**
 * Manager responsible for providing channels and scheduling IO operations.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface NetworkManager {

    /**
     * Binds a channel to the default address and a random port.
     * 
     * @param handler The channel handler.
     * @return The future of the binding operation
     * 
     * @throws IllegalStateException If the manager is inactive.
     */
    ChannelFuture bindDatagramChannel(String address, int port, ChannelHandler handler) throws IllegalStateException;

    /**
     * Activates the manager.
     * 
     * <p>
     * Once activated, the manager will allocate an internal scheduler to handle IO operations and will be able to provide
     * channels.
     * <p>
     * 
     * @throws IllegalStateException If the manager was already activated
     */
    void activate() throws IllegalStateException;

    /**
     * Deactivates the manager.
     * 
     * <p>
     * The internal scheduler will elegantly shutdown any active channel and the manager won't be able to provide channels.
     * <p>
     * 
     * @throws IllegalStateException If the manager is inactive.
     */
    void deactivate() throws IllegalStateException;

    /**
     * Gets whether the manager is active.
     * 
     * @return <code>true</code> if the manager is active; <code>false</code> otherwise.
     */
    boolean isActive();

}
