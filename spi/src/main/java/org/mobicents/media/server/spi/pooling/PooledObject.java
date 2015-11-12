/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.spi.pooling;

/**
 * Represents an object that is managed by a pool.
 * <p>
 * This interface exposes the methods necessary to implement a proper lifecycle for the pooled objects:
 * <ul>
 * <li>guarantees that the objects are properly initialized when polled from the pool.</li>
 * <li>guarantees that the objects are properly closed and reset when pushed into the pool.</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface PooledObject {

    /**
     * Closes and resets the state of the object.
     * <p>
     * Must be invoked before pushing object into the pool.
     * </p>
     */
    void checkIn();

    /**
     * Initializes the object.
     * <p>
     * Must be invoked when polling an object from the pool.
     * </p>
     */
    void checkOut();

}
