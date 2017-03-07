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

package org.restcomm.media.spi.pooling;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstraction of a {@link ResourcePool} that relies on an internal queue to maintain the collection of resources.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractResourcePool<T extends PooledObject> implements ResourcePool<T> {

    private final Queue<T> resources;
    private int initialCapacity;
    private final AtomicInteger size;

    protected AbstractResourcePool(Queue<T> resources, int initialCapacity) {
        this.resources = resources;
        this.initialCapacity = initialCapacity;
        this.size = new AtomicInteger(initialCapacity);
    }

    /**
     * Populates the pool with new objects until the initial capacity is reached.
     * <p>
     * IMPORTANT: This method should be manually invoked by the constructor of each implementation.<br>
     * The reason is that some more complex objects require dependencies or factories.
     * </p>
     */
    protected void populate() {
        for (int index = 0; index < this.initialCapacity; index++) {
            this.resources.offer(createResource());
        }
    }

    @Override
    public T poll() {
        // Get resource
        T resource = resources.poll();
        if (resource == null) {
            resource = createResource();
            this.size.incrementAndGet();
        }

        // Initialize state of the resource
        resource.checkOut();
        return resource;
    }

    @Override
    public void offer(T resource) {
        if (resource != null) {
            // Reset state of the object
            resource.checkIn();

            // Place object back into the pool
            this.resources.offer(resource);
        }
    }

    @Override
    public void release() {
        this.resources.clear();
    }

    @Override
    public int count() {
        return this.resources.size();
    }
    
    @Override
    public int size() {
        return this.size.get();
    }

    @Override
    public boolean isEmpty() {
        return this.resources.isEmpty();
    }

    protected abstract T createResource();

}
