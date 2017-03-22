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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.ice.FoundationsRegistry;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.ice.LocalCandidateWrapper;
import org.restcomm.media.ice.harvest.HarvestException;
import org.restcomm.media.ice.harvest.HostCandidateHarvester;
import org.restcomm.media.ice.harvest.NoCandidatesGatheredException;
import org.restcomm.media.ice.lite.LiteFoundationsRegistry;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateHarvesterTest {

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
		for (LocalCandidateWrapper localCandidate : rtpComponent
				.getLocalCandidates()) {
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
	public void testHostCandidateHarvesting() throws IOException {
		// given
		FoundationsRegistry foundationsRegistry = new LiteFoundationsRegistry();
		HostCandidateHarvester harvester = new HostCandidateHarvester(
				foundationsRegistry);
		IceMediaStream mediaStream = new IceMediaStream("audio", true);

		Selector selector = Selector.open();
		
		// when
		try {
			RtpPortManager portManager = new RtpPortManager(61000, 62000);
			harvester.harvest(portManager, mediaStream, selector);
		} catch (NoCandidatesGatheredException e) {
			fail();
		} catch (HarvestException e) {
			fail();
		}

		// then
		List<LocalCandidateWrapper> rtpCandidates = mediaStream
				.getRtpComponent().getLocalCandidates();
		List<LocalCandidateWrapper> rtcpCandidates = mediaStream
				.getRtcpComponent().getLocalCandidates();

		assertTrue(rtpCandidates.size() > 0);
		assertTrue(rtcpCandidates.size() > 0);
		// Evaluate RTP Candidates
		for (LocalCandidateWrapper candidateWrapper : rtpCandidates) {
			DatagramChannel udpChannel = candidateWrapper.getChannel();
			assertFalse(udpChannel.isBlocking());
			assertFalse(udpChannel.isConnected());
			assertTrue(udpChannel.isOpen());

			IceCandidate candidate = candidateWrapper.getCandidate();
			assertEquals(candidate, candidate.getBase());
			assertEquals(new InetSocketAddress(candidate.getAddress(),
					candidate.getPort()), udpChannel.getLocalAddress());
			assertNotNull(udpChannel.keyFor(selector));
		}
		// Evaluate RTCP candidates
		for (LocalCandidateWrapper candidateWrapper : rtcpCandidates) {
			DatagramChannel udpChannel = candidateWrapper.getChannel();
			assertFalse(udpChannel.isBlocking());
			assertFalse(udpChannel.isConnected());
			assertTrue(udpChannel.isOpen());

			IceCandidate candidate = candidateWrapper.getCandidate();
			assertEquals(candidate, candidate.getBase());
			assertEquals(new InetSocketAddress(candidate.getAddress(),
					candidate.getPort()), udpChannel.getLocalAddress());
			assertNotNull(udpChannel.keyFor(selector));
		}
	}

	// TODO test port lookup for host candidate harvester
	public void testPortLookup() {
		// TODO Bind a socket to an address:port
		// TODO Tell harvester to search on that same port
		// TODO Assert binder binds to the next free port
	}
}
