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
import org.restcomm.media.core.rtcp.RtcpSdesItem;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpSdesItemTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { 0x01, 0x0e, 0x51, 0x54, 0x53, 0x53, 0x31, 0x32, 0x36, 0x32, 0x31, 0x38, 0x39, 0x38,
			0x35, 0x31 };

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
		RtcpSdesItem rtcpSdesItem = new RtcpSdesItem();

		rtcpSdesItem.decode(p, 0);

		assertEquals(1, rtcpSdesItem.getType());
		assertEquals(14, rtcpSdesItem.getLength());
		assertEquals("QTSS1262189851", rtcpSdesItem.getText());

	}

	@Test
	public void testEncode() {
		RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, "QTSS1262189851");
		byte[] rtcp_sdes_item = new byte[320];
		int length = rtcpSdesItem.encode(rtcp_sdes_item, 0);

		assertEquals(p.length, length);

		for(int i=0;i<p.length;i++){
			assertEquals(p[i], rtcp_sdes_item[i]);
		}
	}

}
