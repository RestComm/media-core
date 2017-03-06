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

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.control.mgcp.connection.LocalConnectionFactory;
import org.restcomm.media.control.mgcp.connection.LocalConnectionPool;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.control.mgcp.endpoint.MyTestEndpoint;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.UdpManager;
import org.restcomm.media.resource.player.audio.AudioPlayerFactory;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderPool;
import org.restcomm.media.resources.dtmf.DtmfDetectorFactory;
import org.restcomm.media.resources.dtmf.DtmfDetectorPool;
import org.restcomm.media.resources.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resources.dtmf.DtmfGeneratorPool;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.TooManyConnectionsException;

/**
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class LocalJoiningTest {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    
    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);

    //endpoint and connection
    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;
    private ChannelsManager channelsManager;

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

    Component sine1,sine2;
    Component analyzer1,analyzer2;
    
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
    	//use default clock
        clock = new WallClock();
        //create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(new ServiceScheduler()), dtlsServerProvider);
        channelsManager.setScheduler(mediaScheduler);
        
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
        endpoint1.setFreq(200);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(mediaScheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.setFreq(200);
        endpoint2.start();
        
        sine1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SINE);
    	
    	analyzer1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);    	
    }

    @After
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }

        mediaScheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testFullduplex() throws Exception {
    	long s = System.nanoTime();

        sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL,false);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_RECV);
        connection2.setMode(ConnectionMode.SEND_RECV);
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);    	
    }

    @Test
    public void testHalfDuplex() throws Exception {
    	long s = System.nanoTime();

        sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL,false);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_ONLY);
        connection2.setMode(ConnectionMode.RECV_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);    	
    }
}