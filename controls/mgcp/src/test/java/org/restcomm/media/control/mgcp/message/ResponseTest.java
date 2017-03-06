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
package org.restcomm.media.control.mgcp.message;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.control.mgcp.message.MgcpResponse;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class ResponseTest {
    private byte[] buff = "200 123 Transaction executed normaly\n".getBytes();
    private MgcpResponse response;
    
    public ResponseTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        response = new MgcpResponse();
        response.strain(buff, 0, buff.length);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getResponseCode method, of class Response.
     */
    @Test
    public void testGetResponseCode() {
        assertEquals(200, response.getResponseCode());
    }

    /**
     * Test of getTxID method, of class Response.
     */
    @Test
    public void testGetTxID() {
        assertEquals(123, response.getTxID());
    }

    /**
     * Test of getResponseString method, of class Response.
     */
    @Test
    public void testGetResponseString() {
        assertEquals("Transaction executed normaly", response.getResponseString().toString());
    }

}
