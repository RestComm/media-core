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

package org.mobicents.media.core;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.connections.RtpConnectionImpl;
import org.mobicents.media.core.endpoints.BaseMixerEndpointImpl;
import org.mobicents.media.core.endpoints.impl.IvrEndpoint;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.resource.dtmf.GeneratorImpl;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
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
 */
public class MediaGroupTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //RTP
    private ChannelsManager channelsManager;

    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    //endpoint and connection
    private BaseMixerEndpointImpl endpoint1,endpoint2;
    private ResourcesPool resourcesPool;
    protected UdpManager udpManager;
    
    private String tone;
    
    public MediaGroupTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, IOException {
    	//use default clock
        clock = new DefaultClock();

        //create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(scheduler);        

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
                
        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        //assign scheduler to the endpoint
        endpoint1 = new IvrEndpoint("test");
        endpoint1.setScheduler(scheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.start();
        
        endpoint2 = new IvrEndpoint("test 2");
        endpoint2.setScheduler(scheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.start();    	
    }

    @After
    public void tearDown() {
        endpoint1.deleteAllConnections();
        endpoint2.deleteAllConnections();
        endpoint1.releaseResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        endpoint2.releaseResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        endpoint1.stop();
        endpoint2.stop();
        udpManager.stop();
        scheduler.stop();          
    }

	/**
	 * Test of setOtherParty method, of class RtpConnectionImpl.
	 */
	@Test
	public void testResources() throws Exception {
		Connection connection1 = endpoint1.createConnection(ConnectionType.RTP, false);
		Connection connection2 = endpoint2.createConnection(ConnectionType.RTP, false);

		connection1.generateLocalDescriptor();
		connection2.generateLocalDescriptor();

		Text sd1 = new Text(connection1.getDescriptor());
		Text sd2 = new Text(connection2.getDescriptor());

		connection1.setOtherParty(sd2);
		connection2.setOtherParty(sd1);

		connection1.setMode(ConnectionMode.SEND_RECV);
		connection2.setMode(ConnectionMode.SEND_RECV);

		GeneratorImpl generator = (GeneratorImpl) endpoint1.getResource(MediaType.AUDIO, ComponentType.DTMF_GENERATOR);
		DetectorImpl detector = (DetectorImpl) endpoint2.getResource(MediaType.AUDIO, ComponentType.DTMF_DETECTOR);

		detector.addListener(this);

		generator.setToneDuration(200);
		generator.setVolume(-20);

		generator.setDigit("1");
		generator.activate();
		detector.activate();

		Thread.sleep(1000);

		assertEquals("1", tone);
		generator.deactivate();

		tone = "";
		generator.setOOBDigit("1");
		generator.activate();
		detector.activate();

		Thread.sleep(1000);

		assertEquals("1", tone);
	}

	@Override
    public void process(DtmfEvent event) {
    	tone = event.getTone();
    }    
}