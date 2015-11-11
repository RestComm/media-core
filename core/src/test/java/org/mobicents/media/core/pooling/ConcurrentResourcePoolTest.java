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

import org.junit.Test;

import junit.framework.Assert;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ConcurrentResourcePoolTest {
    
    @Test
    public void testOfferAndPollAndClear() {
        // Given
        ResourcePool<PooledObjectMock> pool = new ConcurrentResourcePool<PooledObjectMock>();
        PooledObjectMock resource1 = new PooledObjectMock();
        PooledObjectMock resource2 = new PooledObjectMock();
        PooledObjectMock resource3 = new PooledObjectMock();
        
        // When
        pool.offer(resource1);
        pool.offer(resource2);
        pool.offer(resource3);
        // Then
        Assert.assertEquals(3, pool.size());
        
        // When
        pool.poll();
        // Then
        Assert.assertEquals(2, pool.size());
        
        // When
        pool.clear();
        Assert.assertEquals(0, pool.size());
    }
    
    @Test
    public void testResourceLifecycle() {
        // Given
        ResourcePool<PooledObjectMock> pool = new ConcurrentResourcePool<PooledObjectMock>();
        PooledObjectMock resource1 = new PooledObjectMock();
        
        // When
        pool.offer(resource1);
        Assert.assertTrue(resource1.isCheckedId());
        Assert.assertFalse(resource1.isCheckedOut());
        
        // Then
        PooledObjectMock polledResource = pool.poll();
        Assert.assertEquals(resource1, polledResource);
        Assert.assertTrue(resource1.isCheckedOut());
    }

}
