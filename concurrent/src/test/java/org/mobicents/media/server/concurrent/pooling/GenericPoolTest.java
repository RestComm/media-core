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

import junit.framework.Assert;

import org.junit.Test;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericPoolTest {

    @Test
    public void testResourceLifecycle() {
        // Given
        final ConcurrentResourcePool<PoolResourceMock> pool = new ConcurrentResourcePool<PoolResourceMock>();
        PoolResourceMock resource1 = new PoolResourceMock();
        
        // When
        pool.offer(resource1);
        
        // Then
        Assert.assertTrue(resource1.isClosed());
        Assert.assertTrue(resource1.isReset());
        
        // When
        resource1 = pool.poll();
        
        // Then
        Assert.assertNotNull(resource1);
        Assert.assertTrue(resource1.isInitialized());
    }

    @Test
    public void testOfferAndPoll() {
        // Given
        final ConcurrentResourcePool<PoolResourceMock> pool = new ConcurrentResourcePool<PoolResourceMock>();
        PoolResourceMock resource1 = new PoolResourceMock();
        PoolResourceMock resource2 = new PoolResourceMock();
        PoolResourceMock resource3 = new PoolResourceMock();
        
        // When
        pool.offer(resource1);
        pool.offer(resource2);
        pool.offer(resource3);
        
        // Then
        Assert.assertEquals(3, pool.size());
        
        // When
        pool.poll();
        pool.poll();
        pool.poll();
        PoolResourceMock resource4 = pool.poll();
        
        // Then
        Assert.assertNull(resource4);
        Assert.assertEquals(0, pool.size());
    }
    
}
