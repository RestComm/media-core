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

package org.mobicents.media.server.impl.rtcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpAppDefinedTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x81, (byte) 0xcc, 0x00, 0x06, 0x0f, (byte) 0xdf, 0x2b, 0x6f, 0x71, 0x74,
			0x73, 0x69, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61, 0x74, 0x00, 0x04, 0x00, 0x00, 0x00, 0x14 };

	public RtcpAppDefinedTest() {

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
		RtcpAppDefined rtcpAppDefined = new RtcpAppDefined();
		int length = rtcpAppDefined.decode(p, 0);

		assertEquals(p.length, length);

		assertEquals(2, rtcpAppDefined.getVersion());
		assertFalse(rtcpAppDefined.isPadding());
		assertEquals(1, rtcpAppDefined.getCount()); // subtype

		assertEquals(RtcpCommonHeader.RTCP_APP, rtcpAppDefined.getPacketType());

		assertEquals(266283887, rtcpAppDefined.getSsrc());

		assertEquals("qtsi", rtcpAppDefined.getName());

		byte[] expected = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61, 0x74, 0x00, 0x04, 0x00,
				0x00, 0x00, 0x14 };
		java.util.Arrays.equals(expected, rtcpAppDefined.getData());

	}

	@Test
	public void testEncode() {

		byte[] appData = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61, 0x74, 0x00, 0x04, 0x00,
				0x00, 0x00, 0x14 };

		RtcpAppDefined rtcpAppDefined = new RtcpAppDefined(false, 1, 266283887, "qtsi", appData);
		

		byte[] rawData = new byte[256];

		int length = rtcpAppDefined.encode(rawData, 0);

		assertEquals(p.length, length);

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}
}
