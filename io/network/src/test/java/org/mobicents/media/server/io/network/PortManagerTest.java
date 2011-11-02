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

package org.mobicents.media.server.io.network;

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
public class PortManagerTest {

    private PortManager portManager = new PortManager();

    public PortManagerTest() {
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
     * Test of setLowestPort method, of class PortManager.
     */
    @Test
    public void testEvenLowestPort() {
        portManager.setLowestPort(2);
        assertEquals(2, portManager.getLowestPort());
    }

    @Test
    public void testOddLowestPort() {
        portManager.setLowestPort(1);
        assertEquals(2, portManager.getLowestPort());
    }

    /**
     * Test of getLowestPort method, of class PortManager.
     */
    @Test
    public void testEvenHighestPort() {
        portManager.setHighestPort(10);
        assertEquals(10, portManager.getHighestPort());
    }

    @Test
    public void testOddHighestPort() {
        portManager.setHighestPort(11);
        assertEquals(10, portManager.getHighestPort());
    }

    /**
     * Test of next method, of class PortManager.
     */
    @Test
    public void testNext() {
        portManager.setLowestPort(2);
        portManager.setHighestPort(10);

        assertEquals(4, portManager.next());
        assertEquals(8, portManager.next());
        assertEquals(4, portManager.next());
        assertEquals(8, portManager.next());
    }

}