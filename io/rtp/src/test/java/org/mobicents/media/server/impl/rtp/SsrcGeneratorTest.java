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
