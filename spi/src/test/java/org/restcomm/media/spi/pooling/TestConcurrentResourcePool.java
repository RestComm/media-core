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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import junit.framework.Assert;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TestConcurrentResourcePool {

    private static final Logger logger = LogManager.getLogger(TestConcurrentResourcePool.class);

    @Test
    public void testPoolPrePopulates() {
        // given
        final int initialSize = 10;

        // when
        final ResourcePool<PooledObjectMock> pool = new ConcurrentResourcePoolMock(initialSize);

        // then
        Assert.assertEquals(initialSize, pool.count());
    }

    @Test
    public void testCreateElementWhenEmpty() {
        // given
        final int initialSize = 0;
        final ResourcePool<PooledObjectMock> pool = new ConcurrentResourcePoolMock(initialSize);

        // when
        PooledObjectMock obj1 = pool.poll();

        // then
        Assert.assertNotNull(obj1);
        Assert.assertTrue(pool.isEmpty());
    }

    @Test
    public void testOfferPoll() {
        // given
        final int initialSize = 0;
        final ResourcePool<PooledObjectMock> pool = new ConcurrentResourcePoolMock(initialSize);
        final PooledObjectMock obj1 = mock(PooledObjectMock.class);

        // when
        pool.offer(obj1);

        // then
        Assert.assertEquals(1, pool.count());
        verify(obj1, times(1)).checkIn();
        
        // when
        PooledObjectMock polledObj = pool.poll();

        // then
        Assert.assertNotNull(polledObj);
        Assert.assertTrue(pool.isEmpty());
        verify(polledObj, times(1)).checkOut();
    }

    @Test
    public void testConcurrentPollOffer() {
        // given
        final int numClients = 5000;
        final int initialCapacity = 10;
        final ResourcePool<PooledObjectMock> resourcePool = new ConcurrentResourcePoolMock(initialCapacity);
        final ExecutorService scheduler = Executors.newFixedThreadPool(5);

        // when
        for (int index = 0; index < numClients; index++) {
            scheduler.submit(new PoolClient(resourcePool));
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("Could not wait for pool clients to execute.", e);
        }

        int count = resourcePool.count();
        logger.info("The pool grew to " + count + " elements.");

        resourcePool.release();
        scheduler.shutdown();

        // then
        Assert.assertTrue(initialCapacity < count);
        Assert.assertTrue(resourcePool.isEmpty());
    }

    private static class PoolClient implements Runnable {

        private static final AtomicInteger counter = new AtomicInteger(0);

        private final int id;
        private final ResourcePool<PooledObjectMock> pool;
        private final List<PooledObjectMock> resources;

        protected PoolClient(ResourcePool<PooledObjectMock> pool) {
            this.id = counter.getAndIncrement();
            this.pool = pool;
            this.resources = new ArrayList<PooledObjectMock>(5);
        }

        @Override
        public void run() {
            // poll from pool
            for (int index = 0; index < 4; index++) {
                logger.info("Client " + this.id + " is polling from pool.");
                this.resources.add(this.pool.poll());
            }

            // offer back to pool
            while (!resources.isEmpty()) {
                logger.info("Client " + this.id + " is offering to pool.");
                this.pool.offer(this.resources.remove(0));
            }
        }

    }

}
