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

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests an SSRC generator for RTP channels.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SsrcGeneratorTest {

	@Test
	public void testUniquenessAndSize() {
		// given
		int iterations = 1000;
		List<Long> ssrcs = new ArrayList<Long>(iterations);

		for (int i = 0; i < iterations; i++) {
			// when
			long ssrc = SsrcGenerator.generateSsrc();
			int size = Long.SIZE - Long.numberOfLeadingZeros(ssrc);
			
			// then
			Assert.assertFalse(ssrcs.contains(Long.valueOf(ssrc)));
			Assert.assertTrue(SsrcGenerator.MAX_SIZE >= size);

			ssrcs.add(ssrc);
		}
	}

}
