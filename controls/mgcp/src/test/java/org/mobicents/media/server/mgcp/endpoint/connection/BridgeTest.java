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

package org.mobicents.media.server.mgcp.endpoint.connection;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderFactory;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorPool;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.CachedRemoteStreamProvider;
import org.mobicents.media.server.mgcp.connection.LocalConnectionFactory;
import org.mobicents.media.server.mgcp.connection.LocalConnectionPool;
import org.mobicents.media.server.mgcp.connection.RtpConnectionFactory;
import org.mobicents.media.server.mgcp.connection.RtpConnectionPool;
import org.mobicents.media.server.mgcp.endpoint.MyTestEndpoint;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.utils.Text;

/**
 * @author oifa yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class BridgeTest extends RTPEnvironment {

    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;
    private MyTestEndpoint endpoint3;
    private MyTestEndpoint endpoint4;

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
    
    Component sine1,sine2,sine3,sine4;
    Component analyzer1,analyzer2,analyzer3,analyzer4;

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

        endpoint4 = new MyTestEndpoint("test-4");
        endpoint4.setScheduler(mediaScheduler);
        endpoint4.setResourcesPool(resourcesPool);
        endpoint4.setFreq(800);
        endpoint4.start();
        
        sine1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine3=endpoint3.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine4=endpoint4.getResource(MediaType.AUDIO,ComponentType.SINE);
    	
    	analyzer1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer3=endpoint3.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer4=endpoint4.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
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
        sine1.activate();
    	sine2.activate();
    	sine3.activate();
    	sine4.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	analyzer3.activate();
    	analyzer4.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.RTP,false);
    	Connection connection3 = endpoint3.createConnection(ConnectionType.RTP,false);
        
    	connection1.generateOffer(false);
    	connection1.setMode(ConnectionMode.SEND_ONLY);
    	
        connection3.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection3.getLocalDescriptor()));

        //make inactive first        
        
        connection3.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.CONFERENCE);

        Connection connection12 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL,false);
        
        connection12.setOtherParty(connection2);
        
        //initial mode
        connection12.setMode(ConnectionMode.SEND_RECV);
        
        //working mode
        connection12.setMode(ConnectionMode.CONFERENCE);
        connection2.setMode(ConnectionMode.SEND_RECV);

        Connection connection34 = endpoint3.createConnection(ConnectionType.LOCAL,false);
        Connection connection4 = endpoint4.createConnection(ConnectionType.LOCAL,false);

        connection34.setOtherParty(connection4);
        connection34.setMode(ConnectionMode.CONFERENCE);
        
        connection4.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(100);
        connection4.setMode(ConnectionMode.SEND_RECV);
        
        Thread.sleep(5000);

        sine1.deactivate();
    	sine2.deactivate();
    	sine3.deactivate();
    	sine4.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	analyzer3.deactivate();
    	analyzer4.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;
        SpectraAnalyzer a3 = (SpectraAnalyzer) analyzer3;
        SpectraAnalyzer a4 = (SpectraAnalyzer) analyzer4;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();
        int[] s3 = a3.getSpectra();
        int[] s4 = a4.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint1.deleteConnection(connection12);
        
        endpoint2.deleteConnection(connection2);

        endpoint3.deleteConnection(connection3);
        endpoint3.deleteConnection(connection34);
        
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