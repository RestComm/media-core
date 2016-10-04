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

package org.mobicents.media.server.mgcp.endpoint;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderFactory;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderPool;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfDetectorPool;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorFactory;
import org.mobicents.media.server.impl.resource.dtmf.DtmfGeneratorPool;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalDetectorPool;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorFactory;
import org.mobicents.media.server.impl.resource.phone.PhoneSignalGeneratorPool;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.mgcp.connection.LocalConnectionFactory;
import org.mobicents.media.server.mgcp.connection.LocalConnectionPool;
import org.mobicents.media.server.mgcp.connection.RtpConnectionFactory;
import org.mobicents.media.server.mgcp.connection.RtpConnectionPool;
import org.mobicents.media.server.mgcp.endpoint.BaseMixerEndpointImpl;
import org.mobicents.media.server.mgcp.endpoint.IvrEndpoint;
import org.mobicents.media.server.mgcp.resources.ResourcesPool;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.utils.Text;

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
    private PhoneSignalDetectorFactory signalDetectorFactory;
    private PhoneSignalDetectorPool signalDetectorPool;
    private PhoneSignalGeneratorFactory signalGeneratorFactory;
    private PhoneSignalGeneratorPool signalGeneratorPool;

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

        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(mediaScheduler);

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");

        // Resource
        this.rtpConnectionFactory = new RtpConnectionFactory(channelsManager, dspFactory);
        this.rtpConnectionPool = new RtpConnectionPool(0, rtpConnectionFactory);
        this.localConnectionFactory = new LocalConnectionFactory(channelsManager);
        this.localConnectionPool = new LocalConnectionPool(0, localConnectionFactory);
        this.playerFactory = new AudioPlayerFactory(mediaScheduler, dspFactory);
        this.playerPool = new AudioPlayerPool(0, playerFactory);
        this.recorderFactory = new AudioRecorderFactory(mediaScheduler);
        this.recorderPool = new AudioRecorderPool(0, recorderFactory);
        this.dtmfDetectorFactory = new DtmfDetectorFactory(mediaScheduler);
        this.dtmfDetectorPool = new DtmfDetectorPool(0, dtmfDetectorFactory);
        this.dtmfGeneratorFactory = new DtmfGeneratorFactory(mediaScheduler);
        this.dtmfGeneratorPool = new DtmfGeneratorPool(0, dtmfGeneratorFactory);
        this.signalDetectorFactory = new PhoneSignalDetectorFactory(mediaScheduler);
        this.signalDetectorPool = new PhoneSignalDetectorPool(0, signalDetectorFactory);
        this.signalGeneratorFactory = new PhoneSignalGeneratorFactory(mediaScheduler);
        this.signalGeneratorPool = new PhoneSignalGeneratorPool(0, signalGeneratorFactory);
        resourcesPool=new ResourcesPool(rtpConnectionPool, localConnectionPool, playerPool, recorderPool, dtmfDetectorPool, dtmfGeneratorPool, signalDetectorPool, signalGeneratorPool);

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