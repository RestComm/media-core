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
public class RTPJoiningTest extends RTPEnvironment {

    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;
    private MyTestEndpoint endpoint3;

    private int count;

    public RTPJoiningTest() {
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

        endpoint3 = new MyTestEndpoint("test-2");
        endpoint3.setScheduler(scheduler);
        endpoint3.setRtpManager(rtpManager);
        endpoint3.setDspFactory(dspFactory);
        endpoint3.setFreq(600);
        endpoint3.start();        
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
        
        super.tearDown();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testSendRecv() throws Exception {
    	long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());

        connection1.setOtherParty(sd2);
        connection2.setOtherParty(sd1);

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.SEND_RECV);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(200, s1[0], 5);
        assertEquals(400, s2[0], 5);
    }

    
    @Test
    public void testNetworkLoop() throws Exception {
    	long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());

        connection1.setOtherParty(sd2);
        connection2.setOtherParty(sd1);

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.NETWORK_LOOPBACK);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(200, s2[0], 5);
//        assertEquals(400, s1[0], 5);
    }
    
    @Test
    public void testSendOnly() throws Exception {
    	long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());

        connection1.setOtherParty(sd2);
        connection2.setOtherParty(sd1);

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(400, s2[0], 5);
    }

    @Test
    public void testRecvOnly() throws Exception {
    	long s = System.nanoTime();

        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());

        connection1.setOtherParty(sd2);
        connection2.setOtherParty(sd1);

        connection2.setMode(ConnectionMode.RECV_ONLY);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);
        
        System.out.println("Check point 8: " + ((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 8));
        System.out.println("Check point 6: " + ((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 6));
        System.out.println("Check point 4: " + ((BaseConnection)connection2).getCheckPoint(4));
        
        System.out.println("s1.length=" + s1.length);
        System.out.println("s2.length=" + s2.length);

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(400, s2[0], 5);        
    }

    @Test
    public void testRxTxChangeMode() throws Exception {
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP);
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());
        connection1.setOtherParty(sd2);        
        connection2.setOtherParty(sd1);
        connection2.setMode(ConnectionMode.RECV_ONLY);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        Thread.sleep(3000);
        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();
        
        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(400, s2[0], 5);
        
        connection2.setMode(ConnectionMode.SEND_ONLY);
        connection1.setMode(ConnectionMode.RECV_ONLY);
        
        Thread.sleep(3000);
        
        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);

        s1 = a1.getSpectra();
        s2 = a2.getSpectra();
        
        assertEquals(1, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(200, s1[0], 5);
    }
    
    @Test
    public void testCnf() throws Exception {
    	long s = System.nanoTime();

        //first leg
        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP);
        Connection connection2 = endpoint3.createConnection(ConnectionType.RTP);
        
        Text sd1 = new Text(connection1.getDescriptor());
        Text sd2 = new Text(connection2.getDescriptor());

        connection1.setOtherParty(sd2);
        connection2.setOtherParty(sd1);

        connection2.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.SEND_RECV);
        
        //second leg
        Connection connection3 = endpoint2.createConnection(ConnectionType.RTP);
        Connection connection4 = endpoint3.createConnection(ConnectionType.RTP);

        Text sd3 = new Text(connection3.getDescriptor());
        Text sd4 = new Text(connection4.getDescriptor());

        connection3.setOtherParty(sd4);
        connection4.setOtherParty(sd3);
        
        connection3.setMode(ConnectionMode.SEND_RECV);
        connection4.setMode(ConnectionMode.CONFERENCE);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);
        
        SpectraAnalyzer a1 = (SpectraAnalyzer) endpoint1.getSink(MediaType.AUDIO);
        SpectraAnalyzer a2 = (SpectraAnalyzer) endpoint2.getSink(MediaType.AUDIO);

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection3);
        endpoint1.deleteConnection(connection1);
        
        try
        {
        	endpoint3.deleteAllConnections();
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
                
//        System.out.println("Check point 8: " + ((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 8));
//        System.out.println("Check point 6: " + ((BaseConnection)connection2).getCheckPoint(MediaType.AUDIO, 6));
//        System.out.println("Check point 4: " + ((BaseConnection)connection2).getCheckPoint(4));
        
//        System.out.println("s1.length=" + s1.length);
//        System.out.println("s2.length=" + s2.length);

        System.out.println("==== spectra");
        for (int i = 0; i < s1.length; i++) {
            System.out.println(s1[i]);
        }
        assertEquals(2, s1.length);
        assertEquals(2, s2.length);
        
        assertEquals(400, s2[0], 5);
    }
    
    
}