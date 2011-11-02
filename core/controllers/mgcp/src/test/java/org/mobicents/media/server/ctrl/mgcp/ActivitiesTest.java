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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.ctrl.mgcp;

import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class ActivitiesTest {
    
    private Activities activities = new Activities(null);
    
    public ActivitiesTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getCall method, of class Activities.
     */
    @Test
    public void testGetCall() {
        Call call = activities.createCall("1");
        assertEquals(1, activities.calls.size());
        call.terminate();
        assertEquals(0, activities.calls.size());
    }

    /**
     * Test of getEndpointActivity method, of class Activities.
     */
    @Test
    public void testGetEndpointActivity() {
        EndpointActivity a = activities.getEndpointActivity(new EndpointIdentifier("1", "2"));
        assertEquals(1, activities.endpoints.size());
        a.terminate();
        assertEquals(1, activities.endpoints.size());
    }

    /**
     * Test of getEndpointActivity method, of class Activities.
     */
    @Test
    public void testConnectionActivity() throws UnknownActivityException {
        Call call = activities.createCall("1");
        
        EndpointActivity ea = activities.getEndpointActivity(new EndpointIdentifier("1", "2"));        
        ConnectionActivity ca = activities.createConnectionActivity("1", new EndpointIdentifier("1", "2"));
        
        assertEquals(1, ea.connections.size());
        assertEquals(1, call.connections.size());

        ConnectionActivity ca1 = ea.getConnectionActivity(ca.getID());
        assertEquals(ca, ca1);
        
        ca.terminate();
        
        assertEquals(0, ea.connections.size());
        assertEquals(0, call.connections.size());
        
    }
    
}