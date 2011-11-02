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

package org.mobicents.javax.media.mscontrol;

import java.util.Properties;
import javax.media.mscontrol.MediaSession;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.javax.media.mscontrol.spi.DriverImpl;

/**
 *
 * @author kulikov
 */
public class MediaObjectImplTest {

    private DriverImpl driver;
    
    private MediaSession session;
    private MsControlFactoryImpl factory;
    
    public MediaObjectImplTest() {
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
    }

    /**
     * Test of getURI method, of class MediaObjectImpl.
     */
    @Test
    public void testGetURI() throws MsControlException {
        session = factory.createMediaSession();
        System.out.println(session.getURI());
        System.out.println(session.createNetworkConnection(NetworkConnection.BASIC).getURI());
    }

}