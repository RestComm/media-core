package org.mobicents.media.io.ice;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class IceMediaStreamTest {

	@Test(expected = IllegalArgumentException.class)
	public void testNullNameValidation() {
		new IceMediaStream(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyNameValidation() {
		new IceMediaStream("");
	}

	@Test
	public void testStreamCreationWithRtcp() {
		// given
		String name = "AuDiO";
		
		// when
		IceMediaStream stream = new IceMediaStream(name);

		// then
		assertEquals(name.toLowerCase(), stream.getName());
		assertNotNull(stream.getRtpComponent());
		assertNotNull(stream.getRtcpComponent());
		assertTrue(stream.supportsRtcp());
		assertFalse(stream.hasLocalRtpCandidates());
		assertFalse(stream.hasLocalRtcpCandidates());
		assertNull(stream.getRemoteUfrag());
		assertNull(stream.getRemotePassword());
	}

	@Test
	public void testStreamCreationWithoutRtcp() {
		// given
		String name = "AuDiO";
		
		// when
		IceMediaStream stream = new IceMediaStream(name, false);
		
		// then
		assertEquals(name.toLowerCase(), stream.getName());
		assertNotNull(stream.getRtpComponent());
		assertNull(stream.getRtcpComponent());
		assertFalse(stream.supportsRtcp());
		assertFalse(stream.hasLocalRtpCandidates());
		assertFalse(stream.hasLocalRtcpCandidates());
		assertNull(stream.getRemoteUfrag());
		assertNull(stream.getRemotePassword());
	}
	
	@Test
	public void testCandidateSelection() {
		// given
		IceMediaStream stream = new IceMediaStream("audio", true);
		IceComponent rtpComponent = stream.getRtpComponent();
		IceCandidate rtpCandidate = new IceCandidate(rtpComponent, "127.0.0.1", 0, CandidateType.HOST);
		LocalCandidateWrapper rtpCandidateWrapper = new LocalCandidateWrapper(rtpCandidate, null);
		
		IceComponent rtcpComponent = stream.getRtcpComponent();
		IceCandidate rtcpCandidate = new IceCandidate(rtcpComponent, "127.0.0.1", 0, CandidateType.HOST);
		LocalCandidateWrapper rtcpCandidateWrapper = new LocalCandidateWrapper(rtcpCandidate, null);
		
		// when
		rtpComponent.addLocalCandidate(rtpCandidateWrapper);
		rtcpComponent.addLocalCandidate(rtcpCandidateWrapper);
		stream.selectLocalDefaultCandidates();
		
		// then
		assertTrue(stream.hasLocalRtpCandidates());
		assertEquals(rtpCandidateWrapper, rtpComponent.getDefaultLocalCandidate());

		assertTrue(stream.hasLocalRtcpCandidates());
		assertEquals(rtcpCandidateWrapper, rtcpComponent.getDefaultLocalCandidate());
	}
	

}
