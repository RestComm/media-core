package org.mobicents.media.core.ice;

import static org.junit.Assert.*;

import org.junit.Test;

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
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		HostCandidate candidate = new HostCandidate(rtpComponent,
				"192.168.1.65", 6100);

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
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		IceComponent rtcpComponent = new IceComponent(IceComponent.RTCP_ID);
		HostCandidate rtpCandidate = new HostCandidate(rtpComponent,
				"192.168.1.65", 6100);
		HostCandidate rtcpCandidate = new HostCandidate(rtcpComponent,
				"192.168.1.65", 6100);

		// when
		rtpCandidate.setPriority(2);
		rtcpCandidate.setPriority(1);

		// then
		assertTrue(rtpCandidate.compareTo(rtcpCandidate) > 0);
		assertTrue(rtpCandidate.compareTo(rtpCandidate) == 0);
		assertTrue(rtcpCandidate.compareTo(rtpCandidate) < 0);
	}
}
