package org.mobicents.media.server.impl.rtp;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa
 *
 */
public class SsrcGeneratorTest {

	@Test
	public void testGenerateSsrc() {
		for (int i = 0; i < 100; i++) {
			System.out.println(SsrcGenerator.generateSsrc());
		}
	}

}
