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
		IceComponent component = new IceComponent(IceComponent.RTP_ID);

		// when
		HostCandidate candidate = new HostCandidate(component, "192.168.1.65",
				6100, "1");
		// then
		assertEquals(CandidateType.HOST, candidate.getType());
		assertEquals(IceCandidate.IP4_PRECEDENCE,
				candidate.getAddressPrecedence());
		assertTrue(candidate.isIPv4());
		long calculatedPriority = calculatePriority(
				CandidateType.HOST.getPreference(),
				candidate.getAddressPrecedence(), component.getComponentId());
		assertEquals(calculatedPriority, candidate.getPriority());
		assertEquals(candidate, candidate.getBase());
	}

	@Test
	public void testCandidateComparison() {
		// given
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		IceComponent rtcpComponent = new IceComponent(IceComponent.RTCP_ID);

		HostCandidate rtpCandidate = new HostCandidate(rtpComponent,
				"192.168.1.65", 6100, "1");
		HostCandidate rtcpCandidate = new HostCandidate(rtcpComponent,
				"192.168.1.65", 6100, "1");

		// when
		int compareRtpToRtcp = rtpCandidate.compareTo(rtcpCandidate);
		int compareRtpToRtp = rtpCandidate.compareTo(rtpCandidate);
		int compareRtcpToRtp = rtcpCandidate.compareTo(rtpCandidate);

		// then
		assertTrue(compareRtpToRtcp > 0);
		assertTrue(compareRtpToRtp == 0);
		assertTrue(compareRtcpToRtp < 0);
	}

	private long calculatePriority(int preference, int precedence,
			int componentId) {
		return (long) (preference << 24) + (long) (precedence << 8)
				+ (long) (256 - componentId);
	}

}
