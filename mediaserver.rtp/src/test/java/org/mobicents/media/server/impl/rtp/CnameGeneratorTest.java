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

package org.mobicents.media.server.impl.rtp;

import java.util.ArrayList;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests validity of generated CNAME to be used in RTP sessions.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CnameGeneratorTest {

	@Test
	public void testCnameGenerator() {
		// given
		int minCnameSize = 98;
		int capacity = 10000;
		List<String> cnameList = new ArrayList<String>(capacity);

		for (int i = 0; i < capacity; i++) {
			// when
			String cname = CnameGenerator.generateCname();

			// then
			// test minimum size
			assertEquals((minCnameSize / 8), Base64.decode(cname).length);
			// test uniqueness
			assertFalse(cnameList.contains(cname));
			cnameList.add(cname);
		}

		// cleanup
		cnameList.clear();
	}

}
