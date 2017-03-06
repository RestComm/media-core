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

package org.restcomm.media.control.mgcp.endpoint.connection;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.control.mgcp.connection.LocalConnectionFactory;
import org.restcomm.media.control.mgcp.connection.LocalConnectionPool;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.control.mgcp.endpoint.MyTestEndpoint;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.resource.player.audio.AudioPlayerFactory;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderPool;
import org.restcomm.media.resources.dtmf.DtmfDetectorFactory;
import org.restcomm.media.resources.dtmf.DtmfDetectorPool;
import org.restcomm.media.resources.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resources.dtmf.DtmfGeneratorPool;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.TooManyConnectionsException;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author oifa yulian
 */
public class RTPJoiningTest extends RTPEnvironment {

    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;
    private MyTestEndpoint endpoint3;

    Component sine1,sine2,sine3;
    Component analyzer1,analyzer2,analyzer3;
    
    // Resources
    private ResourcesPool resourcesPool;
    private RtpConnectionFactory rtpConnectionFactory;
    private RtpConnectionPool rtpConnectionPool;
    private LocalConnectionFactory localConnectionFactory;
    private LocalConnectionPool localConnectionPool;
    private AudioPlayerFactory playerFactory;
    private AudioPlayerPool playerPool;
    private AudioRecorderFactory recorderFactory;
    private AudioRecorderPool recorderPool;
    private DtmfDetectorFactory dtmfDetectorFactory;
    private DtmfDetectorPool dtmfDetectorPool;
    private DtmfGeneratorFactory dtmfGeneratorFactory;
    private DtmfGeneratorPool dtmfGeneratorPool;

    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
    	super.setup();
        
        // Resource
        this.rtpConnectionFactory = new RtpConnectionFactory(channelsManager, dspFactory);
        this.rtpConnectionPool = new RtpConnectionPool(0, rtpConnectionFactory);
        this.localConnectionFactory = new LocalConnectionFactory(channelsManager);
        this.localConnectionPool = new LocalConnectionPool(0, localConnectionFactory);
        this.playerFactory = new AudioPlayerFactory(mediaScheduler, dspFactory, new CachedRemoteStreamProvider(100));
        this.playerPool = new AudioPlayerPool(0, playerFactory);
        this.recorderFactory = new AudioRecorderFactory(mediaScheduler);
        this.recorderPool = new AudioRecorderPool(0, recorderFactory);
        this.dtmfDetectorFactory = new DtmfDetectorFactory(mediaScheduler);
        this.dtmfDetectorPool = new DtmfDetectorPool(0, dtmfDetectorFactory);
        this.dtmfGeneratorFactory = new DtmfGeneratorFactory(mediaScheduler);
        this.dtmfGeneratorPool = new DtmfGeneratorPool(0, dtmfGeneratorFactory);
        resourcesPool=new ResourcesPool(rtpConnectionPool, localConnectionPool, playerPool, recorderPool, dtmfDetectorPool, dtmfGeneratorPool);
        
    	//assign scheduler to the endpoint
        endpoint1 = new MyTestEndpoint("test-1");
        endpoint1.setScheduler(mediaScheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.setFreq(400);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(mediaScheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.setFreq(200);
        endpoint2.start();

        endpoint3 = new MyTestEndpoint("test-3");
        endpoint3.setScheduler(mediaScheduler);
        endpoint3.setResourcesPool(resourcesPool);
        endpoint3.setFreq(600);
        endpoint3.start();
    	
        sine1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine3=endpoint3.getResource(MediaType.AUDIO,ComponentType.SINE);
        
    	analyzer1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer3=endpoint3.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
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

    	sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.SEND_RECV);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

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

    	sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.NETWORK_LOOPBACK);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(200, s2[0], 5);    	
    }
    
    @Test
    public void testSendOnly() throws Exception {
    	long s = System.nanoTime();

    	sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection2.setMode(ConnectionMode.SEND_RECV);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

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

    	sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection2.setMode(ConnectionMode.RECV_ONLY);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint2.deleteConnection(connection2);
        endpoint1.deleteConnection(connection1);
        
        System.out.println("s1.length=" + s1.length);
        System.out.println("s2.length=" + s2.length);

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(400, s2[0], 5);        
    }

    @Test
    public void testRxTxChangeMode() throws Exception {
    	
    	sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));
        
        connection2.setMode(ConnectionMode.RECV_ONLY);
        connection1.setMode(ConnectionMode.SEND_ONLY);
        
        Thread.sleep(3000);
        
        SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();
        
        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        
        assertEquals(400, s2[0], 5);
        
        connection2.setMode(ConnectionMode.SEND_ONLY);
        connection1.setMode(ConnectionMode.RECV_ONLY);
        
        Thread.sleep(3000);
        
        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
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

    	sine1.activate();
    	sine2.activate();
    	sine3.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	analyzer3.activate();
    	
    	//first leg
        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
        Connection connection2 = endpoint3.createConnection(ConnectionType.RTP,false);
        
        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection2.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.SEND_RECV);
        
        //second leg
        Connection connection3 = endpoint2.createConnection(ConnectionType.RTP,false);
        Connection connection4 = endpoint3.createConnection(ConnectionType.RTP,false);

        connection3.generateOffer(false);
        connection4.generateOffer(false);
        
        Text sd3 = new Text(connection3.getDescriptor());
        Text sd4 = new Text(connection4.getDescriptor());

        connection3.setOtherParty(sd4);
        connection4.setOtherParty(sd3);
        
        connection3.setMode(ConnectionMode.SEND_RECV);
        connection4.setMode(ConnectionMode.CONFERENCE);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(3000);
        
        sine1.deactivate();
    	sine2.deactivate();
    	sine3.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	analyzer3.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

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
                
        System.out.println("==== spectra");
        for (int i = 0; i < s1.length; i++) {
            System.out.println(s1[i]);
        }
        assertEquals(2, s1.length);
        assertEquals(2, s2.length);
        
        assertEquals(400, s2[0], 5);
    }
    
    
}