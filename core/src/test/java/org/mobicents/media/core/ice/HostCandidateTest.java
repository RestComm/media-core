package org.mobicents.media.core.ice;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.core.ice.candidate.IceCandidate;

/**
 * Tests an ICE candidate of type HOST.
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidateTest {

	@Test
	public void testHostCandidate() {
		// given
		HostCandidate candidate = new HostCandidate("192.168.1.65", 6100);

		// then
		assertEquals(CandidateType.HOST, candidate.getType());
		assertEquals(IceCandidate.IP4_PRECEDENCE,
				candidate.getAddressPrecedence());
		assertTrue(candidate.isIPv4());
		assertEquals(candidate, candidate.getBase());
	}

	@Test
	public void testCandidateComparison() {
		// given
		HostCandidate rtpCandidate = new HostCandidate("192.168.1.65", 6100);
		HostCandidate rtcpCandidate = new HostCandidate("192.168.1.65", 6100);

		// when
		rtpCandidate.setPriority(2);
		rtcpCandidate.setPriority(1);

		// then
		assertTrue(rtpCandidate.compareTo(rtcpCandidate) > 0);
		assertTrue(rtpCandidate.compareTo(rtpCandidate) == 0);
		assertTrue(rtcpCandidate.compareTo(rtpCandidate) < 0);
	}
}
