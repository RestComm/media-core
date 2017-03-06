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
import org.restcomm.media.ice.HostCandidate;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;

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
		HostCandidate candidate = new HostCandidate(rtpComponent, "192.168.1.65", 6100);

		// then
		assertEquals(CandidateType.HOST, candidate.getType());
		assertEquals(IceCandidate.IP4_PRECEDENCE, candidate.getAddressPrecedence());
		assertTrue(candidate.isIPv4());
		assertEquals(candidate, candidate.getBase());
	}

	@Test
	public void testCandidateComparison() {
		// given
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		IceComponent rtcpComponent = new IceComponent(IceComponent.RTCP_ID);
		HostCandidate rtpCandidate = new HostCandidate(rtpComponent, "192.168.1.65", 6100);
		HostCandidate rtcpCandidate = new HostCandidate(rtcpComponent, "192.168.1.65", 6100);

		// when
		rtpCandidate.setPriority(2);
		rtcpCandidate.setPriority(1);

		// then
		assertTrue(rtpCandidate.compareTo(rtcpCandidate) > 0);
		assertTrue(rtpCandidate.compareTo(rtpCandidate) == 0);
		assertTrue(rtcpCandidate.compareTo(rtpCandidate) < 0);
	}
}
