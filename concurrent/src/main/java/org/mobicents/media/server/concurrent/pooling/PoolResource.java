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

package org.mobicents.media.server.concurrent.pooling;

/**
 * Represents an object that can be pooled.
 * <p>
 * The methods exposed by this interface are meant to represent the object's lifecycle, ensuring that its state is pristine
 * whenever it's retrieved from the pool.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface PoolResource {

    /**
     * Initializes the resource and its dependencies.
     * <p>
     * Must be invoked when the pulled from the pool to ensure the object is ready to be used.
     * </p>
     */
    void checkOut();

    /**
     * Closes the resource and resets its state.
     * <p>
     * Must be invoked before putting the object into the pool, to ensure the object will not use system resources when idle AND
     * that its state is pristine when polled for reusal.
     * </p>
     */
    void checkIn();

}
