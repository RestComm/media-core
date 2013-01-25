/*
 * Telestax, Open Source Cloud Communications
 * Copyright 2013, Telestax, Inc. and individual contributors
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

package org.mobicents.media.server.impl.rtp.rfc2833;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBMixer;
import org.mobicents.media.server.impl.resource.dtmf.DetectorImpl;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.PcapRtpPlayer;
import org.mobicents.media.server.impl.rtp.RTPDataChannel;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;

/**
 * 
 * Tests Out of band DTMF RTP event detection from a recorded RTP capture files
 * 
 * @author oifa yulian
 * @author Ivelin Ivanov <ivelin.ivanov@telestax.com>
 */
public class RTPEventFromPcapTest implements DtmfDetectorListener {

	// clock and scheduler
	private Clock clock;
	private Scheduler scheduler;

	private ChannelsManager channelsManager;
	private UdpManager udpManager;

	// private SpectraAnalyzer analyzer;
	private DetectorImpl detector;

	private RTPDataChannel channel;

	private DspFactoryImpl dspFactory = new DspFactoryImpl();

	private Dsp dsp;

	private AudioMixer audioMixer;
	private OOBMixer oobMixer;
	private AudioComponent inbandDetectorComponent;
	private OOBComponent oobDetectorComponent;

	// collects DTMF tones detected during a test
	private ConcurrentLinkedQueue<DtmfEvent> dtmfEvents = new ConcurrentLinkedQueue<DtmfEvent>();

	// flags whether the test is finished still transmitting packets
	private boolean isTransmissionFinished = false;
	
	// the component that reads pcap files and transmits the RTP packets over UDP
	private PcapRtpPlayer pcapRtpPlayer = null;
	
	// the main thread of a test 
	private Thread parkedThread = null;
	
	@Before
	public void setUp() throws Exception {
		AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
		Formats fmts = new Formats();
		fmts.add(pcma);

		Formats dstFormats = new Formats();
		dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

		dspFactory
				.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
		dspFactory
				.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

		dsp = dspFactory.newProcessor();

		// use default clock
		clock = new DefaultClock();

		// create single thread scheduler
		scheduler = new Scheduler();
		scheduler.setClock(clock);
		scheduler.start();

		udpManager = new UdpManager(scheduler);
		udpManager.start();

		channelsManager = new ChannelsManager(udpManager);
		channelsManager.setScheduler(scheduler);

		detector = new DetectorImpl("dtmf", scheduler);
		// setting the volume to 0 means that we are not interested in detecting
		// inband events
		detector.setVolume(0);
		detector.setDuration(40);
		detector.addListener(this);

		channel = channelsManager.getChannel();
		channel.bind(false);

		channel.setInputDsp(dsp);
		channel.setFormatMap(AVProfile.audio);

		audioMixer = new AudioMixer(scheduler);
		audioMixer.addComponent(channel.getAudioComponent());
		
		oobMixer = new OOBMixer(scheduler);
		oobMixer.addComponent(channel.getOOBComponent());

		inbandDetectorComponent = new AudioComponent(1);
		inbandDetectorComponent.addOutput(detector.getAudioOutput());
		inbandDetectorComponent.updateMode(true, true);
		audioMixer.addComponent(inbandDetectorComponent);
		
		oobDetectorComponent = new OOBComponent(1);
		oobDetectorComponent.addOutput(detector.getOOBOutput());
		oobDetectorComponent.updateMode(true, true);
		oobMixer.addComponent(oobDetectorComponent);

		isTransmissionFinished = false;
	}

	@After
	public void tearDown() {
		dtmfEvents.clear();
		pcapRtpPlayer.close();
		channel.close();
		audioMixer.stop();
		udpManager.stop();
		scheduler.stop();
	}

	private void testOobDetection(String pcapFileName, int maxTransmissionTimeInSeconds) throws IOException, InterruptedException {
		pcapRtpPlayer = new PcapRtpPlayer(udpManager, pcapFileName);
		pcapRtpPlayer.bind(false);
		int receiverPort = channel.getLocalPort();
		InetSocketAddress receiverAddress = new InetSocketAddress(udpManager.getBindAddress(), receiverPort);
		pcapRtpPlayer.setPeer(receiverAddress);
		PcapRtpPlayer.PlayerListener pl = new PlayerListener(); 
		pcapRtpPlayer.setPlayerListener(pl);
		
		int playerPort = pcapRtpPlayer.getLocalPort();
		InetSocketAddress playerAddress = new InetSocketAddress(udpManager.getBindAddress(), playerPort);
		channel.setPeer(playerAddress);

		channel.updateMode(ConnectionMode.SEND_RECV);
		audioMixer.start();
		oobMixer.start();
		detector.activate();
		pcapRtpPlayer.activate();
		
		parkedThread = Thread.currentThread();
		// wait up to 3 seconds for the test to complete
		// the test will proceed either when the packet transmission is finished
		// or after 3 seconds, whichever occurs first
		LockSupport.parkNanos(maxTransmissionTimeInSeconds * 1000000000L);
		
		assertTrue("RTP packet transmission did not finish in time.", isTransmissionFinished);

		// give a little extra time for the receivers to finish processing packets
		Thread.sleep(1000);

		channel.updateMode(ConnectionMode.INACTIVE);
		audioMixer.stop();
		detector.deactivate();
		
	}
	
	@Test
	public void testDetectionOneHashTone() throws Exception {
		testOobDetection("dtmf-oob-one-hash.cap.gz", 3);
		assertEquals("Expected one detected event", 1, dtmfEvents.size());
		String tone = ((DtmfEvent)dtmfEvents.poll()).getTone();
		assertEquals("Expected # tone as event", "#", tone);
	}

	@Test
	public void testDetectionTwoHashTones() throws Exception {
		testOobDetection("dtmf-oob-two-hash.cap.gz", 20);
		assertEquals("Expected two detected events", 2, dtmfEvents.size());
		String tone = ((DtmfEvent)dtmfEvents.poll()).getTone();
		assertEquals("Expected # tone as the first event", "#", tone);
		tone = ((DtmfEvent)dtmfEvents.poll()).getTone();
		assertEquals("Expected # tone as the second event", "#", tone);
	}

	public void process(DtmfEvent event) {
		System.out.println("TONE=" + event.getTone());
		dtmfEvents.offer(event);
	}

	private class PlayerListener implements PcapRtpPlayer.PlayerListener {
		public void playFinished() {
			isTransmissionFinished = true;
			LockSupport.unpark(parkedThread);
		}
	}
}

