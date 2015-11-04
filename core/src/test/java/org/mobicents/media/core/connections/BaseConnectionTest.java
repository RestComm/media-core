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

package org.mobicents.media.core.connections;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.core.MyTestEndpoint;

import static org.junit.Assert.*;

import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;

/**
 *
 * @author yulian oifa
 */
public class BaseConnectionTest implements ConnectionListener {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler scheduler;

    //endpoint and connection
    private BaseConnection connection;
    private MyTestEndpoint endpoint;

    private boolean halfOpenState;
    private boolean openState;
    private boolean nullState;

    private ResourcesPool resourcesPool;

    private ChannelsManager channelsManager;

    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    public BaseConnectionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, IOException, TooManyConnectionsException {
        //use default clock
        clock = new WallClock();
        
        //create single thread scheduler 
        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(new ServiceScheduler()));
        channelsManager.setScheduler(scheduler);        

        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        //assign scheduler to the endpoint
        endpoint = new MyTestEndpoint("test", scheduler, resourcesPool);
        endpoint.start();

        connection = (BaseConnection) endpoint.createConnection(ConnectionType.LOCAL,false);
        connection.addListener(this);
    }

    @After
    public void tearDown() {
        endpoint.releaseConnections();
        
        endpoint.stop();
        scheduler.stop();
    }

    /**
     * Test of getId method, of class BaseConnection.
     */
    @Test
    public void testGetId() {
        //assertEquals("1", connection.getId());
    }

    /**
     * Test of getState method, of class BaseConnection.
     */
//    @Test
    public void testGetState() {
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    /**
     * Test of getEndpoint method, of class BaseConnection.
     */
//    @Test
    public void testGetEndpoint() {
        assertEquals(endpoint, connection.getEndpoint());
    }
    
    /**
     * Test of bind method, of class BaseConnection.
     */
//    @Test
    public void testBind() throws Exception {
//        assertEquals(ConnectionState.NULL, connection.getState());
//        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());
        assertTrue("Listener did not receive event", halfOpenState);
    }

    /**
     * Test of bind method, of class BaseConnection.
     */
//    @Test
    public void testTimeout() throws Exception {
//        assertEquals(ConnectionState.NULL, connection.getState());
//        connection.bind();

        Thread.sleep(10000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    /**
     * Test of join method, of class BaseConnection.
     */
//    @Test
    public void testJoin() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();
        Thread.sleep(500);
        
        connection.open();

        Thread.sleep(1000);
        assertEquals(ConnectionState.OPEN, connection.getState());
        assertTrue("Listener did not receive event", openState);
    }

    /**
     * Test of close method, of class BaseConnection.
     */
//    @Test
    public void testClose() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());

        connection.bind();
        Thread.sleep(500);

        connection.open();
        Thread.sleep(500);
        
        connection.close();
        Thread.sleep(1000);

        assertEquals(ConnectionState.NULL, connection.getState());

        assertTrue("Listener did not receive event", nullState);
    }

    public void process(ConnectionEvent event) {
        if (event.getId() == ConnectionEvent.STATE_CHANGE) {
            BaseConnection conn = (BaseConnection) event.getSource();

            if (conn.getState() == ConnectionState.HALF_OPEN) {
                halfOpenState = true;
            }

            if (conn.getState() == ConnectionState.OPEN) {
                openState = true;
            }

            if (conn.getState() == ConnectionState.NULL) {
                nullState = true;
            }
        }
    }

}