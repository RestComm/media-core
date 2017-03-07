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
import org.restcomm.media.control.mgcp.endpoint.BaseMixerEndpointImpl;
import org.restcomm.media.control.mgcp.endpoint.IvrEndpoint;
import org.restcomm.media.control.mgcp.resources.ResourcesPool;
import org.restcomm.media.core.configuration.DtlsConfiguration;
import org.restcomm.media.network.UdpManager;
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
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.Scheduler;
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
public class MediaGroupTest {

    // clock and scheduler
    private Clock clock;
    private PriorityQueueScheduler mediaScheduler;
    private final Scheduler scheduler = new ServiceScheduler();
    
    //Dtls Server Provider
    protected ProtocolVersion minVersion = ProtocolVersion.DTLSv10;
    protected ProtocolVersion maxVersion = ProtocolVersion.DTLSv12;
    protected CipherSuite[] cipherSuites = new DtlsConfiguration().getCipherSuites();
    protected String certificatePath = DtlsConfiguration.CERTIFICATE_PATH;
    protected String keyPath = DtlsConfiguration.KEY_PATH;
    protected AlgorithmCertificate algorithmCertificate = AlgorithmCertificate.RSA;
    protected DtlsSrtpServerProvider dtlsServerProvider = new DtlsSrtpServerProvider(minVersion, maxVersion, cipherSuites,
            certificatePath, keyPath, algorithmCertificate);

    // RTP
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

    // endpoint and connection
    private BaseMixerEndpointImpl endpoint1, endpoint2;
    protected UdpManager udpManager;

    private String tone;

    @Before
    public void setUp() throws ResourceUnavailableException, IOException, InterruptedException {
        // use default clock
        clock = new WallClock();

        // create single thread scheduler
        mediaScheduler = new PriorityQueueScheduler();
        mediaScheduler.setClock(clock);
        mediaScheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        scheduler.start();
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager, dtlsServerProvider);
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

        // assign scheduler to the endpoint
        endpoint1 = new IvrEndpoint("test");
        endpoint1.setScheduler(mediaScheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.start();
        Thread.sleep(1000);

        endpoint2 = new IvrEndpoint("test 2");
        endpoint2.setScheduler(mediaScheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.start();
        Thread.sleep(1000);
    }

    @After
    public void tearDown() {
        endpoint1.deleteAllConnections();
        endpoint2.deleteAllConnections();
        endpoint1.releaseResource(MediaType.AUDIO, ComponentType.DTMF_GENERATOR);
        endpoint2.releaseResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
        endpoint1.stop();
        endpoint2.stop();
        udpManager.stop();
        scheduler.stop();
        mediaScheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class RtpConnectionImpl.
     */
    @Test
    public void testResources() throws Exception {
        // given
        Connection connection1 = endpoint1.createConnection(ConnectionType.RTP, false);

        Connection connection2 = endpoint2.createConnection(ConnectionType.RTP, false);

        connection1.generateOffer(false);
        connection2.setOtherParty(new Text(connection1.getLocalDescriptor()));
        connection1.setOtherParty(new Text(connection2.getLocalDescriptor()));

        connection1.setMode(ConnectionMode.SEND_RECV);
        connection2.setMode(ConnectionMode.SEND_RECV);

        GeneratorImpl generator = (GeneratorImpl) endpoint1.getResource(MediaType.AUDIO, ComponentType.DTMF_GENERATOR);
        generator.setToneDuration(200);
        generator.setVolume(-20);

        DetectorImpl detector = (DetectorImpl) endpoint2.getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);
        detector.addListener(new MockDtmfDetectorListener());

        // when
        generator.setDigit("1");
        generator.activate();
        detector.activate();

        Thread.sleep(1000);

        // then
        assertEquals("1", tone);
        generator.deactivate();

        tone = "";
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();

        Thread.sleep(1000);

        assertEquals("1", tone);
    }

    private class MockDtmfDetectorListener implements DtmfDetectorListener {

        @Override
        public void process(DtmfEvent event) {
            tone = event.getTone();
        }

    }

}