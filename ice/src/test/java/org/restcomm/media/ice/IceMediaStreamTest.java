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

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.ice.CandidateType;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.ice.LocalCandidateWrapper;

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
