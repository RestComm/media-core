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

package org.mobicents.media.core.endpoints;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class EndpointPoolTest {

    @Test
    public void testOffer() {
        // Given
        EndpointPool pool = new EndpointPool();
        Endpoint bridge1 = new BridgeEndpointMock("/mobicents/bridge/1");
        Endpoint bridge2 = new BridgeEndpointMock("/mobicents/bridge/2");
        Endpoint ivr1 = new IvrEndpointMock("/mobicents/ivr/1");

        // When
        pool.offer(bridge1);
        pool.offer(bridge2);
        pool.offer(ivr1);

        // Then
        Assert.assertEquals(2, pool.countEndpoints(EndpointType.BRIDGE));
        Assert.assertEquals(1, pool.countEndpoints(EndpointType.IVR));
        Assert.assertEquals(0, pool.countEndpoints(EndpointType.CONFERENCE));
    }

    @Test
    public void testPollExistingElements() {
        // Given
        EndpointPool pool = new EndpointPool();
        Endpoint bridge1 = new BridgeEndpointMock("/mobicents/bridge/1");
        Endpoint bridge2 = new BridgeEndpointMock("/mobicents/bridge/2");
        Endpoint ivr1 = new IvrEndpointMock("/mobicents/ivr/1");

        // When
        pool.offer(bridge1);
        pool.offer(bridge2);
        pool.offer(ivr1);

        Endpoint polledBridge = pool.poll(EndpointType.BRIDGE);
        Endpoint polledIvr = pool.poll(EndpointType.IVR);

        // Then
        Assert.assertNotNull(polledBridge);
        Assert.assertNotNull(polledIvr);
        Assert.assertEquals(1, pool.countEndpoints(EndpointType.BRIDGE));
        Assert.assertEquals(0, pool.countEndpoints(EndpointType.IVR));
    }

    @Test
    public void testPollInexistentElements() {
        // Given
        EndpointPool pool = new EndpointPool();

        // When
        Endpoint polledConference = pool.poll(EndpointType.CONFERENCE);

        // Then
        Assert.assertNotNull(polledConference);
    }

    private abstract class EndpointMock extends BaseEndpointImpl {

        public EndpointMock(String localName) {
            super(localName);
        }

        @Override
        public void deleteConnection(int connectionId) {
            connections.remove(connectionId);
        }

        @Override
        public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        }

        @Override
        public void checkIn() {
        }

        @Override
        public void checkOut() {
        }

    }

    private class BridgeEndpointMock extends EndpointMock {

        public BridgeEndpointMock(String localName) {
            super(localName);
        }

        @Override
        public EndpointType getType() {
            return EndpointType.BRIDGE;
        }

    }

    private class IvrEndpointMock extends EndpointMock {

        public IvrEndpointMock(String localName) {
            super(localName);
        }

        @Override
        public EndpointType getType() {
            return EndpointType.IVR;
        }

    }

}
