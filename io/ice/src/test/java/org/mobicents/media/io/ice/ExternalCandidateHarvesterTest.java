package org.mobicents.media.io.ice;

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
import org.mobicents.media.io.ice.harvest.ExternalCandidateHarvester;
import org.mobicents.media.io.ice.harvest.HarvestException;
import org.mobicents.media.io.ice.harvest.HostCandidateHarvester;
import org.mobicents.media.io.ice.harvest.NoCandidatesGatheredException;
import org.mobicents.media.io.ice.lite.LiteFoundationsRegistry;
import org.mobicents.media.server.io.network.PortManager;

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
			PortManager portManager = new PortManager();
			portManager.setHighestPort(62000);
			portManager.setLowestPort(61000);
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
