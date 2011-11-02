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

package org.mobicents.media.server.connection;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mobicents.media.server.MyTestEndpoint;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;

/**
 *
 * @author kulikov
 */
public class LocalJoiningTest {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //endpoint and connection
    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;

    private RTPManager rtpManager;

    private int count;

    public LocalJoiningTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
        //use default clock
        clock = new DefaultClock();

        //create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        rtpManager = new RTPManager(new UdpManager(scheduler));
        rtpManager.setBindAddress("127.0.0.1");
        rtpManager.setScheduler(scheduler);
        
        //assign scheduler to the endpoint
        endpoint1 = new MyTestEndpoint("test-1");
        endpoint1.setScheduler(scheduler);
        endpoint1.setRtpManager(rtpManager);
        endpoint1.setFreq(200);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(scheduler);
        endpoint2.setRtpManager(rtpManager);
        endpoint2.setFreq(200);
        endpoint2.start();

    }

    @After
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }

        scheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testFullduplex() throws Exception {
        long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_RECV);
        connection2.setMode(ConnectionMode.SEND_RECV);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

/*        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 1));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 3));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 5));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 7));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 9));

        System.out.println("---------------");

        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 10));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 8));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 6));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 4));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 2));

        System.out.println("---------------");
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 1));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 3));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 5));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 7));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 9));

        System.out.println("---------------");
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 10));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 8));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 6));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 4));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 2));

        System.out.println("==================");
        System.out.println(endpoint1.getSink(MediaType.AUDIO).getPacketsReceived());
        System.out.println(endpoint2.getSink(MediaType.AUDIO).getPacketsReceived());
*/
        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        //assertEquals(1, s1.length);
        assertEquals(1, s2.length);
    }

    @Test
    public void testHalfDuplex() throws Exception {
        long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_ONLY);
        connection2.setMode(ConnectionMode.RECV_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

/*        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 1));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 3));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 5));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 7));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 9));

        System.out.println("---------------");

        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 10));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 8));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 6));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 4));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 2));

        System.out.println("---------------");
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 1));
        System.out.println(((BaseConnection)connection2).connections.getCheckPoint(MediaType.AUDIO, 3));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 5));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 7));
        System.out.println(((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 9));

        System.out.println("---------------");
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 10));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 8));
        System.out.println(((BaseConnection)connection1).getCheckPoint(MediaType.AUDIO, 6));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 4));
        System.out.println(((BaseConnection)connection1).connections.getCheckPoint(MediaType.AUDIO, 2));

        System.out.println("==================");
        System.out.println(endpoint1.getSink(MediaType.AUDIO).getPacketsReceived());
        System.out.println(endpoint2.getSink(MediaType.AUDIO).getPacketsReceived());
*/
        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        //assertEquals(1, s1.length);
        assertEquals(1, s2.length);
    }
    

}