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

package org.restcomm.javax.media.mscontrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.media.mscontrol.MsControlException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.javax.media.mscontrol.MediaSessionImpl;
import org.restcomm.javax.media.mscontrol.MsControlFactoryImpl;
import org.restcomm.javax.media.mscontrol.spi.DriverImpl;

import jain.protocol.ip.mgcp.message.parms.CallIdentifier;

/**
 *
 * @author kulikov
 */
public class MediaSessionImplTest {

    private DriverImpl driver;
    
    private MediaSessionImpl session;
    private MsControlFactoryImpl factory;
    
    public MediaSessionImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws MsControlException {
        driver = new DriverImpl();
        Properties properties = new Properties();
        properties.put("mgcp.bind.address", "127.0.0.1");
        properties.put("mgcp.server.address", "127.0.0.1");
        properties.put("mgcp.local.port", "1024");
        properties.put("mgcp.server.port", "1025");
        properties.put("driver.test.mode", "true");
        
        factory = (MsControlFactoryImpl) driver.getFactory(properties);
        session = (MediaSessionImpl) factory.createMediaSession();
    }

    @After
    public void tearDown() {
        driver.shutdown();
    }

    /**
     * Test of getCallID method, of class MediaSessionImpl.
     */
    @Test
    public void testGetCallID() {
        CallIdentifier callID = session.getCallID();
        assertTrue("CallID is null", callID != null);
    }

    /**
     * Test of getUniqueHandler method, of class MediaSessionImpl.
     */
    @Test
    public void testGetUniqueHandler() {
        int i1 = session.getUniqueHandler();
        int i2 = session.getUniqueHandler();
        
        assertTrue("Same handler", i1 != i2);
    }

    @Test
    public void testCreateDestroySession() {
        assertEquals(1, factory.getActiveSessions());
        
        session.release();
        assertEquals(0, factory.getActiveSessions());
    }
    
    @Test
    public void testGetDriver() {
        DriverImpl d = session.getDriver();
        assertTrue("Driver is null", d != null);
    }

}