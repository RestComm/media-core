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

package org.mobicents.media.core.pooling;

/**
 * Resource pool for object storage.
 * <p>
 * <b>The pool is responsible for resetting the objects, not the clients</b>. This is an important rule to avoid turning the
 * resources pool into a cesspool.<br>
 * For this reason, all resources managed by the pool must implement the {@link PooledObject} interface.
 * </p>
 * 
 * @param <T> The type of resources managed by the pool.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface ResourcePool<T extends PooledObject> {

    /**
     * Offers an object to be placed into the pool.
     * <p>
     * <b>IMPORTANT:</b> To ensure the well-functioning of the system, the client MUST guarantee that <b>all</b> references to
     * the pooled object have been cleaned.
     * </p>
     * 
     * @param resource The object to be offered to the pool.
     */
    void offer(T resource);

    /**
     * Polls an object from the pool.
     * 
     * @return The polled object.
     */
    T poll();

    /**
     * Counts the number of elements in the pool.
     * 
     * @return The number of elements in the pool.
     */
    int size();

    /**
     * Clears all elements from the pool.
     */
    void clear();

}
