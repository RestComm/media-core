package org.mobicents.media.server.impl.utils;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.impl.utils.XSRandom;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class XSRandomTest {

	@Test
	public void testSpeed() {
		// given
		XSRandom xsRandom = new XSRandom();
		Random random = new Random();

		// when
		long start = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
			random.nextDouble();
		}
		long randomTime = System.nanoTime() - start;

		// when
		start = System.nanoTime();
		for (int i = 0; i < 1000000; i++) {
			xsRandom.nextDouble();
		}
		long xsRandomTime = System.nanoTime() - start;

		// then
		System.out.println("Random took " + randomTime + " ns. XSRandom took " + xsRandomTime + " ns. Tim diff = "+ (xsRandomTime - randomTime));
		Assert.assertTrue(xsRandomTime < randomTime);
	}

}
