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
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CallManagerTest {

    @Test
    public void testCreateCall() {
        // Given
        CallManager callManager = new CallManager();

        // When
        callManager.createCall(1);
        callManager.createCall(2);
        callManager.createCall(3);

        // Then
        Assert.assertNotNull(callManager.getCall(1));
        Assert.assertNotNull(callManager.getCall(2));
        Assert.assertNotNull(callManager.getCall(3));
        Assert.assertEquals(3, callManager.countCalls());
    }

    @Test
    public void testDeleteCall() {
        // Given
        CallManager callManager = new CallManager();

        // When
        callManager.createCall(1);
        callManager.deleteCall(1);

        // Then
        Assert.assertNull(callManager.getCall(1));
        Assert.assertEquals(0, callManager.countCalls());
    }

    @Test
    public void testDeleteCalls() {
        // Given
        CallManager callManager = new CallManager();
        
        // When
        callManager.createCall(1);
        callManager.createCall(2);
        callManager.createCall(3);
        callManager.deleteCalls();
        
        // Then
        Assert.assertNull(callManager.getCall(1));
        Assert.assertNull(callManager.getCall(2));
        Assert.assertNull(callManager.getCall(3));
        Assert.assertEquals(0, callManager.countCalls());
    }
    
    @Test
    public void testDeleteCascadeCalls() {
        // Given
        CallManager callManager = new CallManager();
        
        // When
        MockEndpoint endpoint1 = new MockEndpoint();
        Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL, false);
        Connection connection2 = endpoint1.createConnection(ConnectionType.LOCAL, false);
        
        Call call1 = callManager.createCall(1);
        call1.addEndpoint(endpoint1);
        callManager.deleteCalls();
        
        // Then
        Assert.assertNull(callManager.getCall(1));
        Assert.assertEquals(0, callManager.countCalls());
        Assert.assertNull(call1.getEndpoint(endpoint1.getLocalName()));
        Assert.assertEquals(0, call1.countEndpoints());
        Assert.assertNull(endpoint1.getConnection(connection1.getId()));
        Assert.assertNull(endpoint1.getConnection(connection2.getId()));
        Assert.assertEquals(0, endpoint1.getActiveConnectionsCount());
    }

}
