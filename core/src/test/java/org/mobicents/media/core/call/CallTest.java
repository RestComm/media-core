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

package org.mobicents.media.core.call;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CallTest {
    
    @Test
    public void testAddEndpoint() {
        // Given
        Call call = new Call(1);
        Endpoint endpoint1 = new MockEndpoint();
        Endpoint endpoint2 = new MockEndpoint();

        // When
        call.addEndpoint(endpoint1);
        call.addEndpoint(endpoint2);

        // Then
        Assert.assertEquals(2, call.countEndpoints());
        Assert.assertEquals(endpoint1, call.getEndpoint(endpoint1.getLocalName()));
        Assert.assertEquals(endpoint2, call.getEndpoint(endpoint2.getLocalName()));
    }

    @Test
    public void testDeleteEndpoint() {
        // Given
        Call call = new Call(1);
        Endpoint endpoint1 = new MockEndpoint();
        Endpoint endpoint2 = new MockEndpoint();

        // When
        call.addEndpoint(endpoint1);
        call.addEndpoint(endpoint2);
        call.deleteEndpoint(endpoint1.getLocalName());
        call.deleteEndpoint(endpoint2.getLocalName());

        // Then
        Assert.assertEquals(0, call.countEndpoints());
        Assert.assertNull(call.getEndpoint(endpoint1.getLocalName()));
        Assert.assertNull(call.getEndpoint(endpoint2.getLocalName()));
    }

    @Test
    public void testDeleteUnknownEndpoint() {
        // Given
        Call call = new Call(1);
        Endpoint endpoint1 = new MockEndpoint();
        Endpoint endpoint2 = new MockEndpoint();

        // When
        call.addEndpoint(endpoint1);
        Endpoint deletedEndpoint = call.deleteEndpoint(endpoint2.getLocalName());

        // Then
        Assert.assertEquals(1, call.countEndpoints());
        Assert.assertNull(deletedEndpoint);
    }

    @Test
    public void testDeleteEndpoints() {
        // Given
        Call call = new Call(1);
        Endpoint endpoint1 = new MockEndpoint();
        Endpoint endpoint2 = new MockEndpoint();

        // When
        call.addEndpoint(endpoint1);
        call.addEndpoint(endpoint2);
        call.deleteEndpoints();

        // Then
        Assert.assertEquals(0, call.countEndpoints());
    }
    
    @Test
    public void testDeleteCascadeConnection() {
        // Given
        Call call = new Call(1);
        Endpoint endpoint1 = new MockEndpoint();

        // When
        endpoint1.createConnection(ConnectionType.LOCAL, false);
        endpoint1.createConnection(ConnectionType.LOCAL, false);
        
        // Then
        Assert.assertEquals(2, endpoint1.getActiveConnectionsCount());
        
        // When
        call.addEndpoint(endpoint1);
        call.deleteEndpoint(endpoint1.getLocalName());
        
        // Then
        Assert.assertEquals(0, endpoint1.getActiveConnectionsCount());
    }

}
