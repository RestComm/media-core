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

package org.mobicents.media.control.mgcp.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Test;
import org.mobicents.media.control.mgcp.MgcpCall;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCallTest {

    @Test
    public void testAddRemoveConnection() {
        // given
        MgcpCall call = new MgcpCall(1);
        MgcpConnection connection1 = mock(MgcpConnection.class);
        MgcpConnection connection2 = mock(MgcpConnection.class);
        MgcpConnection connection3 = mock(MgcpConnection.class);

        // when - add connections
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection2.getIdentifier()).thenReturn(2);
        when(connection3.getIdentifier()).thenReturn(3);

        call.addConnection(connection1);
        call.addConnection(connection2);
        call.addConnection(connection3);

        // then
        assertEquals(1, call.getId());
        assertTrue(call.hasConnections());
        assertEquals(3, call.countConnections());
        assertEquals(connection1, call.getConnection(1));
        assertEquals(connection2, call.getConnection(2));
        assertEquals(connection3, call.getConnection(3));
        assertNull(call.getConnection(4));

        // when - remove connections
        MgcpConnection removed1 = call.removeConnection(1);
        MgcpConnection removed2 = call.removeConnection(2);
        MgcpConnection removed3 = call.removeConnection(3);

        // then
        assertEquals(connection1, removed1);
        assertEquals(connection2, removed2);
        assertEquals(connection3, removed3);
        assertEquals(0, call.countConnections());
        assertFalse(call.hasConnections());
    }

    @Test
    public void testRemoveConnections() {
        // given
        MgcpCall call = new MgcpCall(1);
        MgcpConnection connection1 = mock(MgcpConnection.class);
        MgcpConnection connection2 = mock(MgcpConnection.class);
        MgcpConnection connection3 = mock(MgcpConnection.class);

        // when - add connections
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection2.getIdentifier()).thenReturn(2);
        when(connection3.getIdentifier()).thenReturn(3);

        call.addConnection(connection1);
        call.addConnection(connection2);
        List<MgcpConnection> removed = call.removeConnections();
        call.addConnection(connection3);

        // then
        assertTrue(call.hasConnections());
        assertEquals(1, call.countConnections());
        assertEquals(2, removed.size());
        assertTrue(removed.contains(connection1));
        assertTrue(removed.contains(connection2));
        assertFalse(removed.contains(connection3));
        assertNull(call.getConnection(1));
        assertNull(call.getConnection(2));
        assertEquals(connection3, call.getConnection(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddDuplicateConnection() {
        // given
        MgcpCall call = new MgcpCall(1);
        MgcpConnection connection1 = mock(MgcpConnection.class);
        MgcpConnection connection2 = mock(MgcpConnection.class);

        // when - add connections
        when(connection1.getIdentifier()).thenReturn(1);
        when(connection2.getIdentifier()).thenReturn(1);

        call.addConnection(connection1);
        call.addConnection(connection2);
    }

}
