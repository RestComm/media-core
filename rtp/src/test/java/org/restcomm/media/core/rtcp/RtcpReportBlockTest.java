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

package org.restcomm.media.core.rtcp;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.core.rtcp.RtcpReportBlock;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpReportBlockTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { 0x0f, (byte) 0xdf, 0x2b, 0x6f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0x62,
			0x00, 0x00, 0x00, 0x39, (byte) 0xfb, (byte) 0x9c, 0x07, (byte) 0xef, 0x00, 0x04, 0x06, (byte) 0xab };

	public RtcpReportBlockTest() {

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
	 * Test of isValid method, of class RtpPacket.
	 */
	@Test
	public void testDecode() {
		// These values are from wireshark trace
		RtcpReportBlock rr = new RtcpReportBlock();
		int length = rr.decode(p, 0);

		assertEquals(p.length, length);
		assertEquals(266283887, rr.getSsrc());
		assertEquals(0, rr.getFraction());
		assertEquals(0, rr.getLost());
		assertEquals(0, rr.getSeqNumCycle());
		assertEquals(17250, rr.getLastSeq());
		assertEquals(57, rr.getJitter());
		assertEquals(4221306863l, rr.getLsr());
		assertEquals(263851, rr.getDlsr());
	}

	@Test
	public void testEncode() {
		RtcpReportBlock rr = new RtcpReportBlock(266283887, 0, 0, 0, 17250, 57, 4221306863l, 263851);
		byte[] rawData = new byte[256];

		int length = rr.encode(rawData, 0);

		assertEquals(p.length, length);

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}

}
