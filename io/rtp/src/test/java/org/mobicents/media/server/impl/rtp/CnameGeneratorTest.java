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
