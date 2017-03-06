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

package org.restcomm.media.control.mgcp.endpoint.naming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.spi.Endpoint;
import org.restcomm.media.control.mgcp.endpoint.MyTestEndpoint;
import org.restcomm.media.control.mgcp.endpoint.exception.UnknownEndpointException;
import org.restcomm.media.control.mgcp.endpoint.naming.NamingService;

/**
 *
 * @author yulian oifa
 */
public class NamingServiceTest {

    private NamingService naming = new NamingService();
    
    public NamingServiceTest() {
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
     * Test of register method, of class NamingService.
     */
    @Test
    public void testRegister() throws Exception {
        MyTestEndpoint te1 = new MyTestEndpoint("/mobicents/media/1");
        naming.register(te1);

        Endpoint ee = naming.lookup("/mobicents/media/1", false);
        assertEquals(te1, ee);
    }

    @Test
    public void testUnregister() throws Exception  {
        MyTestEndpoint te1 = new MyTestEndpoint("/mobicents/media/1");
        naming.register(te1);

        Endpoint ee = naming.lookup("/mobicents/media/1", false);
        assertEquals(te1, ee);

        naming.unregister(te1);
        try {
            ee = naming.lookup("/mobicents/media/1", false);
        } catch (UnknownEndpointException e) {
            return;
        }

        fail("UnknownEndpointException expected");
    }


    @Test
    public void testQuarantine() throws Exception  {
        MyTestEndpoint te1 = new MyTestEndpoint("/mobicents/media/1");
        MyTestEndpoint te2 = new MyTestEndpoint("/mobicents/media/2");

        naming.register(te1);
        naming.register(te2);

        Endpoint ee = naming.lookup("/mobicents/media/$", true);
        assertEquals(te1, ee);

        ee = naming.lookup("/mobicents/media/$", true);
        assertEquals(te2, ee);
    }

}