package org.mobicents.media.core.ice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.core.ice.candidate.IceCandidate;
import org.mobicents.media.core.ice.candidate.LocalCandidateWrapper;
import org.mobicents.media.core.ice.harvest.HarvestingException;
import org.mobicents.media.core.ice.harvest.NoCandidateBoundException;
import org.mobicents.media.core.ice.lite.LiteFoundationRegistry;

import static org.junit.Assert.*;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateHarvesterTest {
	private List<LocalCandidateWrapper> harvested;

	private void cleanupHarvested() {
		for (LocalCandidateWrapper candidateWrapper : harvested) {
			try {
				DatagramChannel udpChannel = candidateWrapper.getUdpChannel();
				if (udpChannel.isConnected()) {
					udpChannel.disconnect();
				}
				if (udpChannel.isOpen()) {
					udpChannel.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Before
	public void before() {

	}

	@After
	public void after() {
		if (this.harvested != null) {
			cleanupHarvested();
		}
	}

	@Test
	public void testHostCandidateHarvesting() throws IOException {
		// given
		FoundationsRegistry foundationsRegistry = new LiteFoundationRegistry();
		HostCandidateHarvester harvester = new HostCandidateHarvester();

		// when
		try {
			this.harvested = harvester.harvest(61000, foundationsRegistry);
		} catch (NoCandidateBoundException e) {
			fail();
		} catch (HarvestingException e) {
			fail();
		}

		// then
		assertTrue(this.harvested.size() > 0);
		for (LocalCandidateWrapper candidateWrapper : this.harvested) {
			DatagramChannel udpChannel = candidateWrapper.getUdpChannel();
			assertFalse(udpChannel.isBlocking());
			assertFalse(udpChannel.isConnected());
			assertTrue(udpChannel.isOpen());

			IceCandidate candidate = candidateWrapper.getCandidate();
			assertEquals(candidate, candidate.getBase());
			assertEquals(new InetSocketAddress(candidate.getAddress(),
					candidate.getPort()), udpChannel.getLocalAddress());
		}
	}

	// TODO test port lookup for host candidate harvester
	public void testPortLookup() {
		// TODO Bind a socket to an address:port
		// TODO Tell harvester to search on that same port
		// TODO Assert binder binds to the next free port
	}
}
