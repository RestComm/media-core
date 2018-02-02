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

package org.restcomm.media.control.mgcp.call;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;
import org.restcomm.media.control.mgcp.call.MgcpCall;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCallTest {

    @Test
    public void testCallId() {
        // given
        final int callId = 10;
        final MgcpCall call = new MgcpCall(callId);

        // when
        final int callIdDecimal = call.getCallId();
        final String callIdHexadecimal = call.getCallIdHex();

        // then
        assertEquals(callId, callIdDecimal);
        assertTrue(Integer.toHexString(callId).equalsIgnoreCase(callIdHexadecimal));
    }

    @Test
    public void testAddConnection() {
        // given
        final int callId = 10;
        final int ivrConnection1 = 1;
        final int bridgeConnection1 = 1;
        final int bridgeConnection2 = 2;
        final int bridgeConnection3 = 3;
        final String ivrEndpoint = "mobicents/ivr/1";
        final String bridgeEndpoint = "mobicents/bridge/1";

        final MgcpCall call = new MgcpCall(callId);

        // when
        final boolean addedBridge1 = call.addConnection(bridgeEndpoint, bridgeConnection1);
        final boolean addedBridge2 = call.addConnection(bridgeEndpoint, bridgeConnection2);
        final boolean addedBridge3 = call.addConnection(bridgeEndpoint, bridgeConnection3);
        final boolean addedIvr1 = call.addConnection(ivrEndpoint, ivrConnection1);

        final Set<String> endpoints = call.getEndpoints();
        final Set<Integer> ivrConnections = call.getConnections(ivrEndpoint);
        final Set<Integer> bridgeConnections = call.getConnections(bridgeEndpoint);

        // then
        assertTrue(addedBridge1);
        assertTrue(addedBridge2);
        assertTrue(addedBridge3);
        assertTrue(addedIvr1);

        assertEquals(2, endpoints.size());
        assertTrue(endpoints.contains(bridgeEndpoint));
        assertTrue(endpoints.contains(ivrEndpoint));

        assertEquals(1, ivrConnections.size());
        assertTrue(ivrConnections.contains(Integer.valueOf(ivrConnection1)));

        assertEquals(3, bridgeConnections.size());
        assertTrue(bridgeConnections.contains(Integer.valueOf(bridgeConnection1)));
        assertTrue(bridgeConnections.contains(Integer.valueOf(bridgeConnection2)));
        assertTrue(bridgeConnections.contains(Integer.valueOf(bridgeConnection3)));
    }

    @Test
    public void testAddDuplicateConnection() {
        // given
        final int callId = 10;
        final int bridgeConnection1 = 1;
        final String bridgeEndpoint = "mobicents/bridge/1";
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<Integer> bridgeConnections = call.getConnections(bridgeEndpoint);

        final boolean addedFirst = call.addConnection(bridgeEndpoint, bridgeConnection1);
        final boolean addedSecond = call.addConnection(bridgeEndpoint, bridgeConnection1);

        // then
        assertTrue(addedFirst);
        assertFalse(addedSecond);
        assertEquals(1, bridgeConnections.size());
    }

    @Test
    public void testRemoveConnection() {
        // given
        final int callId = 10;
        final int bridgeConnection1 = 1;
        final int bridgeConnection2 = 2;
        final int bridgeConnection3 = 3;
        final String bridgeEndpoint = "mobicents/bridge/1";
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<String> endpoints = call.getEndpoints();
        final Set<Integer> bridgeConnections = call.getConnections(bridgeEndpoint);

        call.addConnection(bridgeEndpoint, bridgeConnection1);
        call.addConnection(bridgeEndpoint, bridgeConnection2);
        call.addConnection(bridgeEndpoint, bridgeConnection3);
        call.removeConnection(bridgeEndpoint, bridgeConnection1);
        call.removeConnection(bridgeEndpoint, bridgeConnection3);

        // then
        assertEquals(1, endpoints.size());
        assertTrue(endpoints.contains(bridgeEndpoint));
        assertEquals(1, bridgeConnections.size());
        assertTrue(bridgeConnections.contains(Integer.valueOf(bridgeConnection2)));

        // when
        call.removeConnection(bridgeEndpoint, bridgeConnection2);

        // then
        assertTrue(endpoints.isEmpty());
        assertTrue(bridgeConnections.isEmpty());
    }

    @Test
    public void testRemoveConnections() {
        // given
        final int callId = 10;
        final int bridgeConnection1 = 1;
        final int bridgeConnection2 = 2;
        final int bridgeConnection3 = 3;
        final String bridgeEndpoint = "mobicents/bridge/1";
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<String> endpoints = call.getEndpoints();
        final Set<Integer> bridgeConnections = call.getConnections(bridgeEndpoint);

        call.addConnection(bridgeEndpoint, bridgeConnection1);
        call.addConnection(bridgeEndpoint, bridgeConnection2);
        call.addConnection(bridgeEndpoint, bridgeConnection3);
        final Set<Integer> removed = call.removeConnections(bridgeEndpoint);

        // then
        assertTrue(endpoints.isEmpty());
        assertTrue(bridgeConnections.isEmpty());
        assertEquals(3, removed.size());
        assertTrue(removed.contains(Integer.valueOf(bridgeConnection1)));
        assertTrue(removed.contains(Integer.valueOf(bridgeConnection2)));
        assertTrue(removed.contains(Integer.valueOf(bridgeConnection3)));
    }

    @Test
    public void testRemoveInexistentConnection() {
        // given
        final int callId = 10;
        final int bridgeConnection1 = 1;
        final int bridgeConnection2 = 2;
        final String bridgeEndpoint = "mobicents/bridge/1";
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<Integer> bridgeConnections = call.getConnections(bridgeEndpoint);

        final boolean added = call.addConnection(bridgeEndpoint, bridgeConnection1);
        final boolean removed = call.removeConnection(bridgeEndpoint, bridgeConnection2);

        // then
        assertTrue(added);
        assertFalse(removed);
        assertEquals(1, bridgeConnections.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableEndpointQuery() {
        // given
        final int callId = 10;
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<String> endpoints = call.getEndpoints();
        endpoints.add("boom!");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableConnectionQuery() {
        // given
        final int callId = 10;
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<Integer> connections = call.getConnections("mobicents/cnf/1");
        connections.add(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableRemoveConnections() {
        // given
        final int callId = 10;
        final int bridgeConnection1 = 1;
        final String bridgeEndpoint = "mobicents/bridge/1";
        final MgcpCall call = new MgcpCall(callId);

        // when
        final Set<Integer> removed = call.removeConnections(bridgeEndpoint);
        removed.add(Integer.valueOf(bridgeConnection1));
    }

}
