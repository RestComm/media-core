/*
 * Copyright 2012, Telestax, Inc. and individual contributors
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

package org.mobicents.media.core.endpoints;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.core.MyTestEndpoint;
import org.mobicents.media.core.ResourcesPool;
import org.mobicents.media.core.connections.RTPEnvironment;
import org.mobicents.media.core.endpoints.impl.BridgeEndpoint;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dtmf.DtmfDetector;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.resource.DtmfGenerator;
import org.mobicents.media.server.utils.Text;

/**
 * Bridge endpoint tests
 * 
 * A bridge end point allows two kinds of connections:
 *   - RTP (for remote RTP media resources) and
 *   - Local (between the bridge end point and other MMS end points).
 *    
 * The bridge end point mixes and forwards media between remote and local connections.
 * Media and events arriving to the bridge end point from remote connections are mixed
 * and forwarded to local connections. Respectively, media and events arriving 
 * to the bridge end point from local connections are mixed and forwarded to remote connections. 
 * 
 * 
 * @author yulian oifa
 * @author Ivelin Ivanov
 */
public class BridgeEndpointTest extends RTPEnvironment {

	// one end point representing a remote RTP resource
	private MyTestEndpoint endpointRTP1;
	
	// two end points representing resources withing the media server such as IVR, ANN or DS0 
	private MyTestEndpoint endpointLocal1, endpointLocal2;
	
	// the bridge end point being tested
	private BridgeEndpoint bridgeEndpoint;

	Component sineRTP1, sineLocal1, sineLocal2;
	Component analyzerRTP1, analyzerLocal1, analyzerLocal2;
	DtmfGenerator dtmfGeneratorRTP1, dtmfGeneratorLocal1, dtmfGeneratorLocal2;
	DtmfDetector dtmfDetectorRTP1, dtmfDetectorLocal1, dtmfDetectorLocal2;
	ConcurrentLinkedQueue<String> tonesReceivedAtRTP1, tonesReceivedAtLocal1, tonesReceivedAtLocal2;

	private ResourcesPool resourcesPool;

	class ADtmfDetectorListener implements DtmfDetectorListener {
		
		Queue<String> tones;

		ADtmfDetectorListener(Queue<String> tonesQueue) {
			this.tones = tonesQueue;
		}
		
		public void process(DtmfEvent event) {
			tones.add(event.getTone());
		}
		
	}
	
	public BridgeEndpointTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() throws ResourceUnavailableException,
			TooManyConnectionsException, IOException, TooManyListenersException {
		super.setup();

		resourcesPool = new ResourcesPool(scheduler, channelsManager,
				dspFactory);

		// assign scheduler to the end points
		
		endpointRTP1 = new MyTestEndpoint("test-ep-RTP1");
		endpointRTP1.setScheduler(scheduler);
		endpointRTP1.setResourcesPool(resourcesPool);
		endpointRTP1.setFreq(80);
		endpointRTP1.start();

		endpointLocal1 = new MyTestEndpoint("test-ep-Local1");
		endpointLocal1.setScheduler(scheduler);
		endpointLocal1.setResourcesPool(resourcesPool);
		endpointLocal1.setFreq(150);
		endpointLocal1.start();

		endpointLocal2 = new MyTestEndpoint("test-ep-Local2");
		endpointLocal2.setScheduler(scheduler);
		endpointLocal2.setResourcesPool(resourcesPool);
		endpointLocal2.setFreq(250);
		endpointLocal2.start();

		bridgeEndpoint = new BridgeEndpoint("test-ep-bridge");
		bridgeEndpoint.setResourcesPool(resourcesPool);
		bridgeEndpoint.setScheduler(scheduler);
		bridgeEndpoint.start();

		sineRTP1 = endpointRTP1.getResource(MediaType.AUDIO, ComponentType.SINE);
		sineLocal1 = endpointLocal1.getResource(MediaType.AUDIO, ComponentType.SINE);
		sineLocal2 = endpointLocal2.getResource(MediaType.AUDIO, ComponentType.SINE);

		analyzerRTP1 = endpointRTP1.getResource(MediaType.AUDIO,
				ComponentType.SPECTRA_ANALYZER);
		analyzerLocal1 = endpointLocal1.getResource(MediaType.AUDIO,
				ComponentType.SPECTRA_ANALYZER);
		analyzerLocal2 = endpointLocal2.getResource(MediaType.AUDIO,
				ComponentType.SPECTRA_ANALYZER);
		
		dtmfGeneratorRTP1 = (DtmfGenerator) endpointRTP1.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_GENERATOR);
		dtmfGeneratorLocal1 = (DtmfGenerator) endpointLocal1.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_GENERATOR);
		dtmfGeneratorLocal2 = (DtmfGenerator) endpointLocal2.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_GENERATOR);
		
		dtmfDetectorRTP1 = (DtmfDetector) endpointRTP1.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_DETECTOR);
		dtmfDetectorLocal1 = (DtmfDetector) endpointLocal1.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_DETECTOR);
		dtmfDetectorLocal2 = (DtmfDetector) endpointLocal2.getResource(MediaType.AUDIO, 
				ComponentType.DTMF_DETECTOR);
		
		dtmfDetectorRTP1.setVolume(-36);
		dtmfDetectorLocal1.setVolume(-36);
		dtmfDetectorLocal2.setVolume(-36);
		
		tonesReceivedAtRTP1 = new ConcurrentLinkedQueue<String>();
		tonesReceivedAtLocal1 = new ConcurrentLinkedQueue<String>();
		tonesReceivedAtLocal2 = new ConcurrentLinkedQueue<String>();
		
		dtmfDetectorRTP1.addListener(new ADtmfDetectorListener(tonesReceivedAtRTP1));
		dtmfDetectorLocal1.addListener(new ADtmfDetectorListener(tonesReceivedAtLocal1));
		dtmfDetectorLocal2.addListener(new ADtmfDetectorListener(tonesReceivedAtLocal2));
		
	}

	@After
	@Override
	public void tearDown() {
		if (endpointRTP1 != null) {
			endpointRTP1.stop();
		}

		if (endpointLocal1 != null) {
			endpointLocal1.stop();
		}

		if (endpointLocal2 != null) {
			endpointLocal2.stop();
		}

		if (bridgeEndpoint != null) {
			bridgeEndpoint.stop();
		}

		super.tearDown();
	}

	@Test
	public void testBridgeBetweenOneRTPAndOneLocalMixOfAudioAndEvents() throws Exception {
		long s = System.nanoTime();

		Connection connectionRTP1 = endpointRTP1.createConnection(ConnectionType.RTP,
				false);
		Connection connectionBepRTP1 = bridgeEndpoint.createConnection(ConnectionType.RTP,
				false);

		Text sd1 = new Text(connectionRTP1.getDescriptor());
		Text sdBep1 = new Text(connectionBepRTP1.getDescriptor());

		connectionRTP1.setOtherParty(sdBep1);
		connectionBepRTP1.setOtherParty(sd1);

		connectionRTP1.setMode(ConnectionMode.SEND_RECV);
		connectionBepRTP1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal1 = endpointLocal1.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal1 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal1.setOtherParty(connectionBepLocal1);

		connectionEPLocal1.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal1.setMode(ConnectionMode.SEND_RECV);

		sineRTP1.activate();
		sineLocal1.activate();

		analyzerRTP1.activate();
		analyzerLocal1.activate();
		
		dtmfDetectorRTP1.activate();
		dtmfDetectorLocal1.activate();

		dtmfGeneratorRTP1.setToneDuration(200);
		dtmfGeneratorRTP1.setVolume(-20);
        
		dtmfGeneratorLocal1.setToneDuration(200);
		dtmfGeneratorLocal1.setVolume(-20);
		
        dtmfGeneratorRTP1.setDigit("A");
		dtmfGeneratorLocal1.setDigit("1");

		dtmfGeneratorRTP1.activate();
		dtmfGeneratorLocal1.activate();

		System.out.println("Duration= " + (System.nanoTime() - s));

		Thread.sleep(3000);

		sineRTP1.deactivate();
		sineLocal1.deactivate();

		analyzerRTP1.deactivate();
		analyzerLocal1.deactivate();

		SpectraAnalyzer a1 = (SpectraAnalyzer) analyzerRTP1;
		SpectraAnalyzer a2 = (SpectraAnalyzer) analyzerLocal1;

		int[] s1 = a1.getSpectra();
		int[] s2 = a2.getSpectra();

		assertEquals(1, s1.length);
		assertEquals(1, s2.length);

		assertEquals(150, s1[0], 5);
		assertEquals(80, s2[0], 5);
		
		dtmfDetectorRTP1.deactivate();
		dtmfDetectorLocal1.deactivate();

		dtmfGeneratorRTP1.deactivate();
		dtmfGeneratorLocal1.deactivate();

		assertEquals(1, tonesReceivedAtRTP1.size());
		assertEquals(true, tonesReceivedAtRTP1.remove("1"));
		
		assertEquals(1, tonesReceivedAtLocal1.size());
		assertEquals(true, tonesReceivedAtLocal1.remove("A"));
		
		//now OOB
		sineRTP1.activate();
		sineLocal1.activate();

		analyzerRTP1.activate();
		analyzerLocal1.activate();
		
		dtmfDetectorRTP1.activate();
		dtmfDetectorLocal1.activate();

		dtmfGeneratorRTP1.setOOBDigit("A");
		dtmfGeneratorLocal1.setOOBDigit("1");

		dtmfGeneratorRTP1.activate();
		dtmfGeneratorLocal1.activate();

		System.out.println("Duration= " + (System.nanoTime() - s));

		Thread.sleep(3000);

		sineRTP1.deactivate();
		sineLocal1.deactivate();

		analyzerRTP1.deactivate();
		analyzerLocal1.deactivate();

		s1 = a1.getSpectra();
		s2 = a2.getSpectra();

		endpointRTP1.deleteConnection(connectionRTP1);
		endpointLocal1.deleteConnection(connectionEPLocal1);
		bridgeEndpoint.deleteAllConnections();

		assertEquals(1, s1.length);
		assertEquals(1, s2.length);

		assertEquals(150, s1[0], 5);
		assertEquals(80, s2[0], 5);
		
		dtmfDetectorRTP1.deactivate();
		dtmfDetectorLocal1.deactivate();

		dtmfGeneratorRTP1.deactivate();
		dtmfGeneratorLocal1.deactivate();

		assertEquals(1, tonesReceivedAtRTP1.size());
		assertEquals(true, tonesReceivedAtRTP1.remove("1"));
		
		assertEquals(1, tonesReceivedAtLocal1.size());
		assertEquals(true, tonesReceivedAtLocal1.remove("A"));			
	}

	@Test
	public void testBridgeBetweenOneRTPAndTwoLocalConnectionsMixOfAudioAndEvents() throws Exception {
		long s = System.nanoTime();

		Connection connectionRTP1 = endpointRTP1.createConnection(ConnectionType.RTP,
				false);
		Connection connectionBepRTP1 = bridgeEndpoint.createConnection(ConnectionType.RTP,
				false);

		Text sd1 = new Text(connectionRTP1.getDescriptor());
		Text sdBep1 = new Text(connectionBepRTP1.getDescriptor());

		connectionRTP1.setOtherParty(sdBep1);
		connectionBepRTP1.setOtherParty(sd1);

		connectionRTP1.setMode(ConnectionMode.SEND_RECV);
		connectionBepRTP1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal1 = endpointLocal1.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal1 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal1.setOtherParty(connectionBepLocal1);

		connectionEPLocal1.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal2 = endpointLocal2.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal2 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal2.setOtherParty(connectionBepLocal2);

		connectionEPLocal2.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal2.setMode(ConnectionMode.SEND_RECV);

		//its impossible to mix 2 tones in audio , they
		//can not be detected simultaniously , use only OOB
		sineRTP1.activate();
		sineLocal1.activate();
		sineLocal2.activate();

		analyzerRTP1.activate();
		analyzerLocal1.activate();
		analyzerLocal2.activate();

		dtmfDetectorRTP1.activate();
		dtmfDetectorLocal1.activate();
		dtmfDetectorLocal2.activate();

		dtmfGeneratorRTP1.setToneDuration(100);
		dtmfGeneratorRTP1.setVolume(-20);
        
		dtmfGeneratorLocal1.setToneDuration(100);
		dtmfGeneratorLocal1.setVolume(-20);
		
		dtmfGeneratorLocal2.setToneDuration(100);
		dtmfGeneratorLocal2.setVolume(-20);
		
        dtmfGeneratorRTP1.setOOBDigit("A");
		dtmfGeneratorLocal1.setOOBDigit("1");
		dtmfGeneratorLocal2.setOOBDigit("2");

		dtmfGeneratorRTP1.activate();
		dtmfGeneratorLocal1.activate();
		dtmfGeneratorLocal2.activate();		

		System.out.println("Duration= " + (System.nanoTime() - s));

		Thread.sleep(3000);

		sineRTP1.deactivate();
		sineLocal1.deactivate();
		sineLocal2.deactivate();

		analyzerRTP1.deactivate();
		analyzerLocal1.deactivate();
		analyzerLocal2.deactivate();

		SpectraAnalyzer aRTP1 = (SpectraAnalyzer) analyzerRTP1;
		SpectraAnalyzer aLocal1 = (SpectraAnalyzer) analyzerLocal1;
		SpectraAnalyzer aLocal2 = (SpectraAnalyzer) analyzerLocal2;

		int[] sRTP1 = aRTP1.getSpectra();
		int[] sLocal1 = aLocal1.getSpectra();
		int[] sLocal2 = aLocal2.getSpectra();

		endpointRTP1.deleteConnection(connectionRTP1);
		endpointLocal1.deleteConnection(connectionEPLocal1);
		endpointLocal2.deleteConnection(connectionEPLocal1);
		bridgeEndpoint.deleteAllConnections();

		// test that the RTP connection received mixed audio from both Local connections
		assertEquals(2, sRTP1.length);
		assertEquals(150, sRTP1[0], 5);
		assertEquals(250, sRTP1[1], 5);

		// test that each Local connection received the audio from the RTP connection
		assertEquals(1, sLocal1.length);
		assertEquals(1, sLocal2.length);
		assertEquals(80, sLocal1[0], 5);
		assertEquals(80, sLocal2[0], 5);

		dtmfDetectorRTP1.deactivate();
		dtmfDetectorLocal1.deactivate();
		dtmfDetectorLocal2.deactivate();
		
		dtmfGeneratorRTP1.deactivate();
		dtmfGeneratorLocal1.deactivate();
		dtmfGeneratorLocal2.deactivate();
		
		assertEquals(2, tonesReceivedAtRTP1.size());
		assertEquals(true, tonesReceivedAtRTP1.remove("1"));
		assertEquals(true, tonesReceivedAtRTP1.remove("2"));
		
		assertEquals(1, tonesReceivedAtLocal1.size());
		assertEquals(true, tonesReceivedAtLocal1.remove("A"));

		assertEquals(1, tonesReceivedAtLocal2.size());
		assertEquals(true, tonesReceivedAtLocal2.remove("A"));
	}

	@Test
	public void testBridgeBetweenOneRTPAndTwoLocalConnectionsAudioOnly() throws Exception {
		long s = System.nanoTime();

		Connection connectionRTP1 = endpointRTP1.createConnection(ConnectionType.RTP,
				false);
		Connection connectionBepRTP1 = bridgeEndpoint.createConnection(ConnectionType.RTP,
				false);

		Text sd1 = new Text(connectionRTP1.getDescriptor());
		Text sdBep1 = new Text(connectionBepRTP1.getDescriptor());

		connectionRTP1.setOtherParty(sdBep1);
		connectionBepRTP1.setOtherParty(sd1);

		connectionRTP1.setMode(ConnectionMode.SEND_RECV);
		connectionBepRTP1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal1 = endpointLocal1.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal1 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal1.setOtherParty(connectionBepLocal1);

		connectionEPLocal1.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal2 = endpointLocal2.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal2 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal2.setOtherParty(connectionBepLocal2);

		connectionEPLocal2.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal2.setMode(ConnectionMode.SEND_RECV);

		// test audio flow
		sineRTP1.activate();
		sineLocal1.activate();
		sineLocal2.activate();

		analyzerRTP1.activate();
		analyzerLocal1.activate();
		analyzerLocal2.activate();
		
		System.out.println("Duration= " + (System.nanoTime() - s));

		Thread.sleep(3000);

		sineRTP1.deactivate();
		sineLocal1.deactivate();
		sineLocal2.deactivate();

		analyzerRTP1.deactivate();
		analyzerLocal1.deactivate();
		analyzerLocal2.deactivate();

		SpectraAnalyzer aRTP1 = (SpectraAnalyzer) analyzerRTP1;
		SpectraAnalyzer aLocal1 = (SpectraAnalyzer) analyzerLocal1;
		SpectraAnalyzer aLocal2 = (SpectraAnalyzer) analyzerLocal2;

		int[] sRTP1 = aRTP1.getSpectra();
		int[] sLocal1 = aLocal1.getSpectra();
		int[] sLocal2 = aLocal2.getSpectra();

		endpointRTP1.deleteConnection(connectionRTP1);
		endpointLocal1.deleteConnection(connectionEPLocal1);
		endpointLocal2.deleteConnection(connectionEPLocal1);
		bridgeEndpoint.deleteAllConnections();

		// test that the RTP connection received mixed audio from both Local connections
		assertEquals(2, sRTP1.length);
		assertEquals(150, sRTP1[0], 5);
		assertEquals(250, sRTP1[1], 5);

		// test that each Local connection received the audio from the RTP connection
		assertEquals(1, sLocal1.length);
		assertEquals(1, sLocal2.length);
		assertEquals(80, sLocal1[0], 5);
		assertEquals(80, sLocal2[0], 5);
	}


	@Test
	public void testBridgeBetweenOneRTPAndTwoLocalConnectionsEventsOnly() throws Exception {
		long s = System.nanoTime();

		Connection connectionRTP1 = endpointRTP1.createConnection(ConnectionType.RTP,
				false);
		Connection connectionBepRTP1 = bridgeEndpoint.createConnection(ConnectionType.RTP,
				false);

		Text sd1 = new Text(connectionRTP1.getDescriptor());
		Text sdBep1 = new Text(connectionBepRTP1.getDescriptor());

		connectionRTP1.setOtherParty(sdBep1);
		connectionBepRTP1.setOtherParty(sd1);

		connectionRTP1.setMode(ConnectionMode.SEND_RECV);
		connectionBepRTP1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal1 = endpointLocal1.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal1 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal1.setOtherParty(connectionBepLocal1);

		connectionEPLocal1.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal1.setMode(ConnectionMode.SEND_RECV);

		Connection connectionEPLocal2 = endpointLocal2.createConnection(ConnectionType.LOCAL,
				false);
		Connection connectionBepLocal2 = bridgeEndpoint.createConnection(ConnectionType.LOCAL,
				false);

		connectionEPLocal2.setOtherParty(connectionBepLocal2);

		connectionEPLocal2.setMode(ConnectionMode.SEND_RECV);
		connectionBepLocal2.setMode(ConnectionMode.SEND_RECV);

		//its impossible to mix 2 tones in audio , they
		//can not be detected simultaniously , use only OOB
		dtmfGeneratorRTP1.setToneDuration(100);
		dtmfGeneratorRTP1.setVolume(-20);
        
		dtmfGeneratorLocal1.setToneDuration(100);
		dtmfGeneratorLocal1.setVolume(-20);
		
		dtmfGeneratorLocal2.setToneDuration(100);
		dtmfGeneratorLocal2.setVolume(-20);
		
        dtmfDetectorRTP1.activate();
		dtmfDetectorLocal1.activate();
		dtmfDetectorLocal2.activate();

		dtmfGeneratorRTP1.setOOBDigit("A");
		dtmfGeneratorLocal1.setOOBDigit("1");
		dtmfGeneratorLocal2.setOOBDigit("2");

		dtmfGeneratorRTP1.activate();
		dtmfGeneratorLocal2.activate();	
		dtmfGeneratorLocal1.activate();

		System.out.println("Duration= " + (System.nanoTime() - s));

		Thread.sleep(3000);

		endpointRTP1.deleteConnection(connectionRTP1);
		endpointLocal1.deleteConnection(connectionEPLocal1);
		endpointLocal2.deleteConnection(connectionEPLocal1);
		bridgeEndpoint.deleteAllConnections();

		dtmfDetectorRTP1.deactivate();
		dtmfDetectorLocal1.deactivate();
		dtmfDetectorLocal2.deactivate();
		
		dtmfGeneratorRTP1.deactivate();
		dtmfGeneratorLocal1.deactivate();
		dtmfGeneratorLocal2.deactivate();
		
		assertEquals(2, tonesReceivedAtRTP1.size());
		assertEquals(true, tonesReceivedAtRTP1.remove("1"));
		assertEquals(true, tonesReceivedAtRTP1.remove("2"));

		assertEquals(1, tonesReceivedAtLocal1.size());
		assertEquals(true, tonesReceivedAtLocal1.remove("A"));

		assertEquals(1, tonesReceivedAtLocal2.size());
		assertEquals(true, tonesReceivedAtLocal2.remove("A"));
	}

}