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
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class BridgeTest extends RTPEnvironment {

    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;
    private MyTestEndpoint endpoint3;
    private MyTestEndpoint endpoint4;

    private int count;

    public BridgeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
        super.setup();
        
        //assign scheduler to the endpoint
        endpoint1 = new MyTestEndpoint("test-1");
        endpoint1.setScheduler(scheduler);
        endpoint1.setRtpManager(rtpManager);
        endpoint1.setDspFactory(dspFactory);
        endpoint1.setFreq(400);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(scheduler);
        endpoint2.setRtpManager(rtpManager);
        endpoint2.setDspFactory(dspFactory);
        endpoint2.setFreq(200);
        endpoint2.start();

        endpoint3 = new MyTestEndpoint("test-3");
        endpoint3.setScheduler(scheduler);
        endpoint3.setRtpManager(rtpManager);
        endpoint3.setDspFactory(dspFactory);
        endpoint3.setFreq(600);
        endpoint3.start();

        endpoint4 = new MyTestEndpoint("test-4");
        endpoint4.setScheduler(scheduler);
        endpoint4.setRtpManager(rtpManager);
        endpoint4.setDspFactory(dspFactory);
        endpoint4.setFreq(800);
        endpoint4.start();
        
    }

    @After
    @Override
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }

        if (endpoint3 != null) {
            endpoint3.stop();
        }

        if (endpoint4 != null) {
            endpoint4.stop();
        }
        
        super.tearDown();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testTransmission() throws Exception {
        long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        Connection connection3 = endpoint3.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection3.getDescriptor());

        connection1.setOtherParty(sd2);        
        connection3.setOtherParty(sd1);

        //make inactive first        
        
        connection3.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.CONFERENCE);
        
        Connection connection12 = endpoint1.createConnection(ConnectionType.LOCAL);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL);
        
        connection12.setOtherParty(connection2);
        //initial mode
        connection12.setMode(ConnectionMode.SEND_RECV);
        //working mode
        connection12.setMode(ConnectionMode.CONFERENCE);
        
        connection2.setMode(ConnectionMode.SEND_RECV);

        Connection connection32 = endpoint3.createConnection(ConnectionType.LOCAL);
        Connection connection4 = endpoint4.createConnection(ConnectionType.LOCAL);

        connection32.setOtherParty(connection4);
        connection32.setMode(ConnectionMode.CONFERENCE);
        
        connection4.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(100);
        connection4.setMode(ConnectionMode.SEND_RECV);
        
        Thread.sleep(5000);

        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);
        SpectraAnalyzer a3 = (SpectraAnalyzer) endpoint3.getSink(MediaType.AUDIO);
        SpectraAnalyzer a4 = (SpectraAnalyzer) endpoint4.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();
        int[] s3 = a3.getSpectra();
        int[] s4 = a4.getSpectra();

        System.out.println("Check point 10: " + ((BaseConnection)connection3).getCheckPoint(MediaType.AUDIO, 10));
        System.out.println("Check point 7: " + ((BaseConnection)connection3).getCheckPoint(MediaType.AUDIO, 7));
        System.out.println("Check point 5: " + ((BaseConnection)connection3).getCheckPoint(MediaType.AUDIO, 5));
        System.out.println("Check point 3: " + ((BaseConnection)connection3).getCheckPoint(3));
        System.out.println("Check point 1: " + ((BaseConnection)connection3).getCheckPoint(1));
        System.out.println("Check point 2: " + ((BaseConnection)connection3).getCheckPoint(2));
        
        endpoint1.deleteConnection(connection1);
        endpoint1.deleteConnection(connection12);
        
        endpoint2.deleteConnection(connection2);

        endpoint3.deleteConnection(connection3);
        endpoint3.deleteConnection(connection32);
        
        endpoint4.deleteConnection(connection4);
        
        printSpectra("E1", s1);
        printSpectra("E2", s2);
        printSpectra("E3", s3);
        printSpectra("E4", s4);
        
        
        System.out.println("===============");

        assertEquals(3, s2.length);
        assertEquals(3, s4.length);

    }

    
    private void printSpectra(String title, int[]s) {
        System.out.println(title);
        for (int i = 0; i < s.length; i++) {
            System.out.print(s[i] + " ");
        }
        System.out.println();
    }
    
}