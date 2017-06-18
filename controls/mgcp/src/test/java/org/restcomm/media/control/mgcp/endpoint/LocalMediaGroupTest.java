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

package org.restcomm.media.control.mgcp.endpoint;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.ComponentType;
import org.restcomm.media.codec.g711.alaw.Decoder;
import org.restcomm.media.codec.g711.alaw.Encoder;
import org.restcomm.media.component.dsp.DspFactoryImpl;
import org.restcomm.media.control.mgcp.connection.LocalConnectionFactory;
import org.restcomm.media.control.mgcp.connection.LocalConnectionPool;
import org.restcomm.media.control.mgcp.connection.RtpConnectionFactory;
import org.restcomm.media.control.mgcp.connection.RtpConnectionPool;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.network.deprecated.UdpManager;
import org.restcomm.media.resource.dtmf.DetectorImpl;
import org.restcomm.media.resource.dtmf.DtmfDetectorFactory;
import org.restcomm.media.resource.dtmf.DtmfDetectorPool;
import org.restcomm.media.resource.dtmf.DtmfGeneratorFactory;
import org.restcomm.media.resource.dtmf.DtmfGeneratorPool;
import org.restcomm.media.resource.dtmf.GeneratorImpl;
import org.restcomm.media.resource.player.audio.AudioPlayerFactory;
import org.restcomm.media.resource.player.audio.AudioPlayerPool;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.recorder.audio.AudioRecorderFactory;
import org.restcomm.media.resource.recorder.audio.AudioRecorderPool;
import org.restcomm.media.rtp.ChannelsManager;
import org.restcomm.media.rtp.crypto.AlgorithmCertificate;
import org.restcomm.media.rtp.crypto.CipherSuite;
import org.restcomm.media.rtp.crypto.DtlsSrtpServerProvider;
import org.restcomm.media.rtp.jitter.FixedJitterBuffer;
import org.restcomm.media.rtp.jitter.JitterBufferFactory;
import org.restcomm.media.rtp.jitter.JitterBufferFactoryImpl;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.dtmf.DtmfEvent;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class LocalMediaGroupTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    protected ServiceScheduler scheduler = new ServiceScheduler();
    
    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);
    protected JitterBufferFactory jitterBufferFactory = new JitterBufferFactoryImpl(60, FixedJitterBuffer.class.getName(), null);

    //RTP
    private ChannelsManager channelsManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();

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
    
    //endpoint and connection
    private BaseMixerEndpointImpl endpoint1,endpoint2;
    private BridgeEndpoint endpoint3;
    protected UdpManager udpManager;
    
    private String tone;

    @Before
    public void setUp() throws ResourceUnavailableException, IOException {
    	//use default clock
        clock = new WallClock();

        //create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        udpManager = new UdpManager(scheduler, new RtpPortManager(), new RtpPortManager());
        udpManager.setBindAddress("127.0.0.1");
        scheduler.start();
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager, dtlsServerProvider, jitterBufferFactory);
        channelsManager.setScheduler(mediaScheduler);        

        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());
                
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
        endpoint1 = new IvrEndpoint("test");
        endpoint1.setScheduler(mediaScheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.start();
        
        endpoint2 = new IvrEndpoint("test 2");
        endpoint2.setScheduler(mediaScheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.start();    	
        
        endpoint3 = new BridgeEndpoint("test 3");
        endpoint3.setScheduler(mediaScheduler);
        endpoint3.setResourcesPool(resourcesPool);
        endpoint3.start();
    }

    @After
    public void tearDown() {
        endpoint1.deleteAllConnections();
        endpoint2.deleteAllConnections();
        endpoint3.deleteAllConnections();
        endpoint1.releaseResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        endpoint2.releaseResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        endpoint1.stop();
        endpoint2.stop();
        endpoint3.stop();
        udpManager.stop();
        scheduler.stop();
        mediaScheduler.stop();          
    }

    /**
     * Test of setOtherParty method, of class RtpConnectionImpl.
     */
    @Test
    public void testResources() throws Exception {
    	Connection localConnection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);        
        Connection localConnection2 = endpoint3.createConnection(ConnectionType.LOCAL,false);       
        
        localConnection1.setOtherParty(localConnection2);        
        
        localConnection1.setMode(ConnectionMode.SEND_RECV);
        localConnection2.setMode(ConnectionMode.SEND_RECV);
        
        Connection rtpConnection1 = endpoint3.createConnection(ConnectionType.RTP,false);        
        Connection rtpConnection2 = endpoint2.createConnection(ConnectionType.RTP,false);       
        
        rtpConnection1.generateOffer(false);
        rtpConnection2.setOtherParty(new Text(rtpConnection1.getLocalDescriptor()));
        rtpConnection1.setOtherParty(new Text(rtpConnection2.getLocalDescriptor()));
        
        rtpConnection1.setMode(ConnectionMode.SEND_RECV);
        rtpConnection2.setMode(ConnectionMode.SEND_RECV);
        
        GeneratorImpl generator1=(GeneratorImpl)endpoint1.getResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        GeneratorImpl generator2=(GeneratorImpl)endpoint2.getResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        DetectorImpl detector1=(DetectorImpl)endpoint2.getResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        DetectorImpl detector2=(DetectorImpl)endpoint1.getResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        
        detector1.addListener(this);
        detector2.addListener(this);
        
        generator1.setToneDuration(200);
        generator1.setVolume(-20);
        
        generator1.setDigit("1");
        generator1.activate();
        detector1.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);        
        generator1.deactivate();
        
        tone="";
        generator1.setOOBDigit("1");
        generator1.activate();
        detector1.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);
        
        tone="";
        generator2.setToneDuration(200);
        generator2.setVolume(-20);
        
        generator2.setDigit("1");
        generator2.activate();
        detector2.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);        
        generator2.deactivate();
        
        tone="";
        generator2.setOOBDigit("1");
        generator2.activate();
        detector2.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);
    }

    public void process(DtmfEvent event) {
    	tone = event.getTone();
    }    
}