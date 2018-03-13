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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.core.rtcp.RtcpHeader;
import org.restcomm.media.core.rtcp.RtcpSdes;
import org.restcomm.media.core.rtcp.RtcpSdesChunk;
import org.restcomm.media.core.rtcp.RtcpSdesItem;

/**
 * 
 * @author amit bhayani
 *
 */
public class RtcpSdesTest {

	public RtcpSdesTest() {
		// TODO Auto-generated constructor stub
	}

	// These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x81, (byte) 0xca, 0x00, 0x06, 0x56, 0x53, 0x34, 0x46, 0x01, 0x0e, 0x51,
			0x54, 0x53, 0x53, 0x31, 0x32, 0x36, 0x32, 0x31, 0x38, 0x39, 0x38, 0x35, 0x31, 0x00, 0x00, 0x00, 0x00 };

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

	@Test
	public void testDecode() {
		RtcpSdes rtcpSdes = new RtcpSdes();

		rtcpSdes.decode(p, 0);

		assertEquals(2, rtcpSdes.getVersion());
		assertFalse(rtcpSdes.isPadding());
		assertEquals(1, rtcpSdes.getCount());

		assertEquals(RtcpHeader.RTCP_SDES, rtcpSdes.getPacketType());

		assertEquals(28, rtcpSdes.getLength());

		RtcpSdesChunk rtcpSdesChunk = rtcpSdes.getSdesChunks()[0];

		assertNotNull(rtcpSdesChunk);

		assertEquals(1448293446, rtcpSdesChunk.getSsrc());
		assertEquals(2, rtcpSdesChunk.getItemCount());

		RtcpSdesItem rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[0];

		assertNotNull(rtcpSdesItem);

		assertEquals(RtcpSdesItem.RTCP_SDES_CNAME, rtcpSdesItem.getType());
		assertEquals(14, rtcpSdesItem.getLength());
		assertEquals("QTSS1262189851", rtcpSdesItem.getText());

		rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[1];
		assertNotNull(rtcpSdesItem);
		assertEquals(RtcpSdesItem.RTCP_SDES_END, rtcpSdesItem.getType());

	}

	@Test
	public void testEncode() {

		RtcpSdes rtcpSdes = new RtcpSdes(false);

		RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, "QTSS1262189851");

		RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk(1448293446);
		rtcpSdesChunk.addRtcpSdesItem(rtcpSdesItem);
		
		rtcpSdes.addRtcpSdesChunk(rtcpSdesChunk);

		byte[] rawData = new byte[256];

		int length = rtcpSdes.encode(rawData, 0);

		assertEquals(p.length, length);

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}
}
