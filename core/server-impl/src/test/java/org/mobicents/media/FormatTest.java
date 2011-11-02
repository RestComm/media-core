/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.format.AudioFormat;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * 
 * @author Oleg Kulikov
 */
public class FormatTest extends TestCase {

	public FormatTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of matches method, of class Format.
	 */
	@Test
	public void testMatches() {
		Format format1 = new AudioFormat("ALAW", 8000, 8, 1);
		Format format2 = new AudioFormat("alaw", 8000, 8, 1);

		boolean res = format1.matches(format2);
		assertEquals(res, true);

		AudioFormat DTMF1 = new AudioFormat("telephone-event", 8000, AudioFormat.NOT_SPECIFIED,
				AudioFormat.NOT_SPECIFIED);
		AudioFormat DTMF2 =  new AudioFormat("telephone-event", 8000, AudioFormat.NOT_SPECIFIED,
				AudioFormat.NOT_SPECIFIED);

		res = DTMF1.equals(DTMF2);
		assertEquals(res, true);
	}

	@Test
	public void testDTMF() {
		assertEquals(true, AVProfile.DTMF.equals(AVProfile.DTMF));
	}
}