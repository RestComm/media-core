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

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.ice.HostCandidate;
import org.restcomm.media.ice.IceCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.LocalCandidateWrapper;

public class IceComponentTest {

	List<LocalCandidateWrapper> localCandidates = new ArrayList<LocalCandidateWrapper>();

	@Before
	public void before() throws IOException {
		localCandidates.add(buildCandidate("192.168.1.65", 61000, 1));
		localCandidates.add(buildCandidate("192.168.1.65", 61001, 2));
		localCandidates.add(buildCandidate("192.168.1.65", 61003, 3));
		Collections.sort(this.localCandidates);
	}

	@After
	public void after() {
		clearLocalCandidates();
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidComponentId() {
		// given
		short invalidComponentId = 3;
		
		// when
		new IceComponent(invalidComponentId);
	}

	@Test
	public void testDefaultLocalCandidateSelection() {
		// given
		IceComponent component = new IceComponent(IceComponent.RTP_ID);
		component.addLocalCandidates(this.localCandidates);

		// when
		LocalCandidateWrapper defaultCandidate = component.selectDefaultLocalCandidate();

		// then
		assertEquals(this.localCandidates.get(0), defaultCandidate);
	}

	private LocalCandidateWrapper buildCandidate(String address, int port,
			int priority) throws IOException {
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		IceCandidate candidate = new HostCandidate(rtpComponent, address, port);
		DatagramChannel channel = DatagramChannel.open();
		candidate.setPriority(priority);
		return new LocalCandidateWrapper(candidate, channel);
	}

	private void clearLocalCandidates() {
		for (LocalCandidateWrapper candidateWrapper : this.localCandidates) {
			try {
				DatagramChannel udpChannel = candidateWrapper.getChannel();
				if (udpChannel.isConnected()) {
					udpChannel.disconnect();
				}
				if (udpChannel.isOpen()) {
					udpChannel.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
		}
		this.localCandidates.clear();
	}
}
