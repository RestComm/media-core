/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.ice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.ice.FoundationsRegistry;
import org.restcomm.media.ice.HostCandidate;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.ice.LocalCandidateWrapper;
import org.restcomm.media.ice.ServerReflexiveCandidate;
import org.restcomm.media.ice.harvest.ExternalCandidateHarvester;
import org.restcomm.media.ice.harvest.HarvestException;
import org.restcomm.media.ice.harvest.HostCandidateHarvester;
import org.restcomm.media.ice.harvest.NoCandidatesGatheredException;
import org.restcomm.media.ice.lite.LiteFoundationsRegistry;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ExternalCandidateHarvesterTest {

	private IceMediaStream mediaStream;

	@Before
	public void before() {

	}

	@After
	public void after() {
		if (this.mediaStream != null) {
			closeMediaStream(mediaStream);
		}
	}
	
	private void closeMediaStream(IceMediaStream mediaStream) {
		IceComponent rtpComponent = mediaStream.getRtpComponent();
		for (LocalCandidateWrapper localCandidate : rtpComponent.getLocalCandidates()) {
			DatagramChannel channel = localCandidate.getChannel();
			if (channel.isOpen()) {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Test
	public void testCandidateHarvestingNoRtcp() throws IOException {
		// given
		InetAddress externalAddress = Inet4Address.getByName("127.0.0.1");
		FoundationsRegistry foundations = new LiteFoundationsRegistry();

		// TODO also test RTCP candidates harvesting - hrosa
		IceMediaStream mediaStream = new IceMediaStream("audio", false);

		HostCandidateHarvester hostHarvester = new HostCandidateHarvester(foundations);
		ExternalCandidateHarvester srflxHarvester = new ExternalCandidateHarvester(foundations, externalAddress);

		Selector selector = Selector.open();
		
		// when
		try {
			RtpPortManager portManager = new RtpPortManager(61000, 62000);
			// host harvester takes precedence over srflx
			hostHarvester.harvest(portManager, mediaStream, selector);
			srflxHarvester.harvest(portManager, mediaStream, selector);
		} catch (NoCandidatesGatheredException e) {
			fail();
		} catch (HarvestException e) {
			fail();
		}

		// then
		List<LocalCandidateWrapper> rtpCandidates = mediaStream.getRtpComponent().getLocalCandidates();
//		List<LocalCandidateWrapper> rtcpCandidates = mediaStream.getRtcpComponent().getLocalCandidates();

		assertTrue(rtpCandidates.size() > 0);
//		assertTrue(rtcpCandidates.isEmpty());
		
		// Evaluate RTP Candidates
		for (LocalCandidateWrapper candidateWrapper : rtpCandidates) {
			// Host candidates have their own test cases
			// We are only interested on SRFLX candidates
			if(candidateWrapper.getCandidate() instanceof ServerReflexiveCandidate) {
				DatagramChannel udpChannel = candidateWrapper.getChannel();
				assertFalse(udpChannel.isBlocking());
				assertFalse(udpChannel.isConnected());
				assertTrue(udpChannel.isOpen());
				
				ServerReflexiveCandidate candidate = (ServerReflexiveCandidate) candidateWrapper.getCandidate();
				IceCandidate base = candidate.getBase();
				assertTrue(base instanceof HostCandidate);
				assertEquals(base.getPort(), candidate.getPort());
				assertNotSame(new InetSocketAddress(candidate.getAddress(),candidate.getPort()), udpChannel.getLocalAddress());
				assertNotNull(udpChannel.keyFor(selector));
			}
		}
	}
	
}
