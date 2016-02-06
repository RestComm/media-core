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
