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

package org.restcomm.media.ice.lite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.restcomm.media.ice.HostCandidate;
import org.restcomm.media.ice.IceComponent;
import org.restcomm.media.ice.lite.LiteFoundationsRegistry;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class LiteFoundationRegistryTest {

	/**
	 * Tests how the Lite implementation depends solely on the address of a host
	 * candidate to generate a new foundation.
	 */
	@Test
	public void testFoundationRegistry() {
		// given
		LiteFoundationsRegistry registry = new LiteFoundationsRegistry();
		IceComponent rtpComponent = new IceComponent(IceComponent.RTP_ID);
		HostCandidate candidate1 = new HostCandidate(rtpComponent,"192.168.1.65", 61000);
		HostCandidate candidate2 = new HostCandidate(rtpComponent,"192.168.1.66", 61001);
		HostCandidate candidate3 = new HostCandidate(rtpComponent,"192.168.1.65", 61002);

		// when
		String foundation1 = registry.assignFoundation(candidate1);
		String foundation2 = registry.assignFoundation(candidate2);
		String foundation3 = registry.assignFoundation(candidate3);

		// then
		assertEquals(String.valueOf(1), foundation1);
		assertEquals(String.valueOf(2), foundation2);
		assertEquals(String.valueOf(1), foundation3);
	}

}
