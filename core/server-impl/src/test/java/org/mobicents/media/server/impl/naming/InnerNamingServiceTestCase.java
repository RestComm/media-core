/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.server.impl.naming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;

/**
 * 
 * @author amit bhayani
 *
 */
public class InnerNamingServiceTestCase {

    private InnerNamingService innerNamingService = null;

    public InnerNamingServiceTestCase() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException {
        innerNamingService = new InnerNamingService();

        for (int i = 0; i < 9; i++) {
            innerNamingService.addEndpoint(new EndpointImpl("/mobicents/media/aap/" + i, EndpointState.BUSY));
        }
        innerNamingService.addEndpoint(new EndpointImpl("/mobicents/media/aap/9", EndpointState.READY));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class NameParser.
     */
    @Test
    public void testFindAny() {
        try {
            Endpoint endPt = innerNamingService.lookup("/mobicents/media/aap/$", false);
            assertNotNull(endPt);
            assertEquals("/mobicents/media/aap/9", endPt.getLocalName());
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
            assertTrue("Failed to get free endpoint: " + e, false);
        }
    }

    @Test
    public void testFindSpecificFree() {
        try {
            Endpoint endPt = innerNamingService.lookup("/mobicents/media/aap/9", false);
            assertNotNull(endPt);
            assertEquals("/mobicents/media/aap/9", endPt.getLocalName());
            assertEquals(EndpointState.READY, endPt.getState());
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
            assertTrue("Failed to get free endpoint: " + e, false);
        }

        Endpoint endPt = null;
        try {
            endPt = innerNamingService.lookup("/mobicents/media/aap/8", false);
            assertNull(endPt);
        } catch (ResourceUnavailableException e) {
            //expected
            assertTrue("Exception raised when looking up BUSY Endpoint but wanting free ", true);
        }
    }

    @Test
    public void testFindSpecificBusy() {
        try {
            Endpoint endPt = innerNamingService.lookup("/mobicents/media/aap/8", true);
            assertNotNull(endPt);
            assertEquals("/mobicents/media/aap/8", endPt.getLocalName());
            assertEquals(EndpointState.BUSY, endPt.getState());
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
            assertTrue("Failed to get free endpoint: " + e, false);
        }

        try {
            Endpoint endPt = innerNamingService.lookup("/mobicents/media/aap/9", true);
            assertNotNull(endPt);
            assertEquals("/mobicents/media/aap/9", endPt.getLocalName());
            assertEquals(EndpointState.READY, endPt.getState());
        } catch (ResourceUnavailableException e) {
            e.printStackTrace();
            assertTrue("Failed to get free endpoint: " + e, false);
        }
    }

    @Test
    public void testFindAll() throws ResourceUnavailableException {
        Endpoint[] endpoints = innerNamingService.lookupall("/mobicents/media/aap/*");
        assertNotNull(endpoints);
        assertEquals(10, endpoints.length);

    }

    class EndpointImpl implements Endpoint {

        private String localName = null;
        private EndpointState state;

        EndpointImpl(String localName, EndpointState state) {
            this.localName = localName;
            this.state = state;
        }

        public void addNotificationListener(NotificationListener listener) {
        }

        public Connection createConnection() throws TooManyConnectionsException, ResourceUnavailableException {
            return null;
        }

        public Connection createLocalConnection() throws TooManyConnectionsException, ResourceUnavailableException {
            return null;
        }

        public void deleteAllConnections() {
        }

        public void deleteConnection(String connectionID) {
        }

        public String describe(MediaType mediaType) throws ResourceUnavailableException {
            return null;
        }

        public String getLocalName() {
            return localName;
        }

        public Collection<MediaType> getMediaTypes() {
            return null;
        }

        public MediaSink getSink(MediaType media) {
            return null;
        }

        public MediaSource getSource(MediaType media) {
            return null;
        }

        public EndpointState getState() {
            return state;
        }

        public void removeNotificationListener(NotificationListener listener) {
        }

        public void start() throws ResourceUnavailableException {
        }

        public void stop() {
        }

        public long lastTimeUsed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
