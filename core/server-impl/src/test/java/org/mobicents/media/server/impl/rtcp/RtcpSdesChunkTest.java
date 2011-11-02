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
import static org.junit.Assert.assertNotNull;

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
public class RtcpSdesChunkTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { 0x0d, (byte) 0xe9, 0x4f, 0x1f, 0x01, 0x18, 0x61, 0x62, 0x68, 0x61, 0x79, 0x61,
			0x6e, 0x69, 0x40, 0x61, 0x62, 0x68, 0x61, 0x79, 0x61, 0x6e, 0x69, 0x2d, 0x6c, 0x61, 0x70, 0x74, 0x6f, 0x70,
			0x00, 0x00 };

	public RtcpSdesChunkTest() {

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
		RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk();
		rtcpSdesChunk.decode(p, 0);

		assertEquals(233393951, rtcpSdesChunk.getSsrc());
		assertEquals(2, rtcpSdesChunk.getItemCount());

		RtcpSdesItem rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[0];

		assertNotNull(rtcpSdesItem);

		assertEquals(RtcpSdesItem.RTCP_SDES_CNAME, rtcpSdesItem.getType());
		assertEquals(24, rtcpSdesItem.getLength());
		assertEquals("abhayani@abhayani-laptop", rtcpSdesItem.getText());

		rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[1];
		assertNotNull(rtcpSdesItem);
		assertEquals(RtcpSdesItem.RTCP_SDES_END, rtcpSdesItem.getType());
	}

	@Test
	public void testEncode() {
		RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, "abhayani@abhayani-laptop");

		RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk(233393951);
		rtcpSdesChunk.addRtcpSdesItem(rtcpSdesItem);

		byte[] rawData = new byte[256];
		int length = rtcpSdesChunk.encode(rawData, 0);

		assertEquals(p.length, length);

		for(int i=0;i<p.length;i++){
			assertEquals(p[i], rawData[i]);
		}

	}
}
