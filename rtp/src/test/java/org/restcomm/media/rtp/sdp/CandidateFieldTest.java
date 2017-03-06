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

package org.restcomm.media.rtp.sdp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mobicents.media.server.utils.Text;
import org.restcomm.media.rtp.sdp.CandidateField;

import static org.junit.Assert.*;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@Deprecated
public class CandidateFieldTest {

	private final Text CANDIDATE_HOST  = new Text("a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0");
	private final Text CANDIDATE_RELAY = new Text("a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr 85.241.121.60 rport 55027 generation 0");
	private final Text CANDIDATE_SRFLX = new Text("a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0");
	
	@Test
	public void testCandidateHost() {
		CandidateField candidateField = new CandidateField(CANDIDATE_HOST);
		assertEquals(candidateField.getFoundation(), new Text("1995739850"));
		assertEquals(candidateField.getComponentId(), new Text("1"));
		assertEquals(candidateField.getProtocol(), new Text("udp"));
		assertEquals(candidateField.getWeight(), new Text("2113937151"));
		assertEquals(candidateField.getAddress(), new Text("192.168.1.65"));
		assertEquals(candidateField.getPort(), new Text("54550"));
		assertEquals(candidateField.getType(), new Text("host"));
		assertEquals(candidateField.getGeneration(), new Text("0"));
		assertNull(candidateField.getRelatedAddress());
		assertNull(candidateField.getRelatedPort());
		assertEquals(candidateField.toString(), CANDIDATE_HOST.toString());
	}

	@Test
	public void testCandidateSrflx() {
		CandidateField candidateField = new CandidateField(CANDIDATE_SRFLX);
		assertEquals(candidateField.getFoundation(), new Text("2162486046"));
		assertEquals(candidateField.getComponentId(), new Text("1"));
		assertEquals(candidateField.getProtocol(), new Text("udp"));
		assertEquals(candidateField.getWeight(), new Text("1845501695"));
		assertEquals(candidateField.getAddress(), new Text("85.241.121.60"));
		assertEquals(candidateField.getPort(), new Text("60495"));
		assertEquals(candidateField.getType(), new Text("srflx"));
		assertEquals(candidateField.getRelatedAddress(), new Text("192.168.1.65"));
		assertEquals(candidateField.getRelatedPort(), new Text("54550"));
		assertEquals(candidateField.getGeneration(), new Text("0"));
		assertEquals(candidateField.toString(), CANDIDATE_SRFLX.toString());
	}

	@Test
	public void testCandidateRelay() {
		CandidateField candidateField = new CandidateField(CANDIDATE_RELAY);
		assertEquals(candidateField.getFoundation(), new Text("2564697628"));
		assertEquals(candidateField.getComponentId(), new Text("1"));
		assertEquals(candidateField.getProtocol(), new Text("udp"));
		assertEquals(candidateField.getWeight(), new Text("33562367"));
		assertEquals(candidateField.getAddress(), new Text("75.126.93.124"));
		assertEquals(candidateField.getPort(), new Text("53056"));
		assertEquals(candidateField.getType(), new Text("relay"));
		assertEquals(candidateField.getRelatedAddress(), new Text("85.241.121.60"));
		assertEquals(candidateField.getRelatedPort(), new Text("55027"));
		assertEquals(candidateField.getGeneration(), new Text("0"));
		assertEquals(candidateField.toString(), CANDIDATE_RELAY.toString());
	}
	
	@Test
	public void testCandidateComparison() {
		List<CandidateField> candidates = new ArrayList<CandidateField>(3);
		// weight = 33562367
		CandidateField candidateRelay = new CandidateField(CANDIDATE_RELAY);
		// weight = 2113937151
		CandidateField candidateHost = new CandidateField(CANDIDATE_HOST);
		// weight = 1845501695
		CandidateField candidateSrflx = new CandidateField(CANDIDATE_SRFLX);

		candidates.add(candidateHost);
		candidates.add(candidateRelay);
		candidates.add(candidateSrflx);
		Collections.sort(candidates, Collections.reverseOrder());
		assertEquals(candidates.get(0), candidateHost);
		assertEquals(candidates.get(1), candidateSrflx);
		assertEquals(candidates.get(2), candidateRelay);
	}
	
}
