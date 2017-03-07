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

package org.restcomm.media.spi.pooling;

/**
 * Represents a resource pool that manages a collection of {@link PooledObject}.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface ResourcePool<T extends PooledObject> {

    /**
     * Polls an object from the pool.<br>
     * If the pool is empty, a new object shall be created.
     * 
     * @return An object from the pool.
     */
    T poll();

    /**
     * Offers an object to be placed into the pool.
     * 
     * @param resource The object to be placed into the pool.
     */
    void offer(T resource);

    /**
     * Releases all resources held by the pool.
     */
    void release();

    /**
     * Counts the number of idle elements in the pool.
     * 
     * @return the number of elements in the pool.
     */
    int count();

    /**
     * Gets the maximum size of the pool.
     * 
     * @return The pool size
     */
    int size();

    /**
     * Checks whether the pool contains at least one element.
     * 
     * @return Returns <code>true</code> is empty, <code>false</code> otherwise.
     */
    boolean isEmpty();

}
