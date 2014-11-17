package org.mobicents.media.server.impl.utils;

import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.impl.utils.XSRandom;

public class XSRandomTest {

	@Test
	public void testSpeed() {
		// given
		XSRandom xsRandom = new XSRandom();
		Random random = new Random();

		// when
		long start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			random.nextDouble();
		}
		long randomTime = System.currentTimeMillis() - start;

		// when
		start = System.currentTimeMillis();
		for (int i = 0; i < 10000; i++) {
			xsRandom.nextDouble();
		}
		long xsRandomTime = System.currentTimeMillis() - start;

		// then
		System.out.println("Random took " + randomTime + " ms. XSRandom took " + xsRandomTime + " ms.");
		Assert.assertTrue(xsRandomTime < randomTime);
	}

}
