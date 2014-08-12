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
public class RtcpPacketTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x80, (byte) 0xc8, 0x00, 0x06, 0x0f, (byte) 0xdf, 0x2b, 0x6f, (byte) 0xce,
			(byte) 0xe5, (byte) 0xfb, (byte) 0x9c, 0x07, (byte) 0xef, (byte) 0x9d, (byte) 0xb2, 0x5e, (byte) 0x90,
			(byte) 0x83, (byte) 0xbb, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0b, (byte) 0x81, (byte) 0xca, 0x00,
			0x06, 0x0f, (byte) 0xdf, 0x2b, 0x6f, 0x01, 0x0e, 0x51, 0x54, 0x53, 0x53, 0x31, 0x32, 0x36, 0x32, 0x31,
			0x38, 0x39, 0x38, 0x35, 0x31, 0x00, 0x00, 0x00, 0x00, (byte) 0x81, (byte) 0xcc, 0x00, 0x06, 0x0f,
			(byte) 0xdf, 0x2b, 0x6f, 0x71, 0x74, 0x73, 0x69, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61,
			0x74, 0x00, 0x04, 0x00, 0x00, 0x00, 0x14 };

	private byte[] p1 = new byte[] { (byte) 0x80, (byte) 0xc9, 0x00, 0x01, (byte) 0xcc, (byte) 0xcb, (byte) 0x96,
			(byte) 0xc6, (byte) 0x81, (byte) 0xca, 0x00, 0x07, (byte) 0xcc, (byte) 0xcb, (byte) 0x96, (byte) 0xc6,
			0x01, 0x12, 0x61, 0x62, 0x68, 0x61, 0x79, 0x61, 0x6e, 0x69, 0x40, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x68, 0x6f,
			0x73, 0x74, 0x00, 0x00, 0x00, 0x00 };

	public RtcpPacketTest() {

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
		RtcpPacket rtcpPacket = new RtcpPacket();
		int length = rtcpPacket.decode(p, 0);

		assertEquals(p.length, length);

		// SR
		RtcpSenderReport rtcpSenderReport = rtcpPacket.getRtcpSenderReport();
		assertEquals(2, rtcpSenderReport.getVersion());
		assertFalse(rtcpSenderReport.isPadding());
		assertEquals(0, rtcpSenderReport.getCount());

		assertEquals(RtcpCommonHeader.RTCP_SR, rtcpSenderReport.getPacketType());

		assertEquals(28, rtcpSenderReport.getLength());

		assertEquals(266283887, rtcpSenderReport.getSsrc());

		assertEquals(3471178652l, rtcpSenderReport.getNtpSec());
		assertEquals(133143986, rtcpSenderReport.getNtpFrac());

		assertEquals(1586529211, rtcpSenderReport.getRtpTs());

		assertEquals(1, rtcpSenderReport.getPsent());

		assertEquals(11, rtcpSenderReport.getOsent());

		// SDES
		RtcpSdes rtcpSdes = rtcpPacket.getRtcpSdes();
		assertEquals(2, rtcpSdes.getVersion());
		assertFalse(rtcpSdes.isPadding());
		assertEquals(1, rtcpSdes.getCount());

		assertEquals(RtcpCommonHeader.RTCP_SDES, rtcpSdes.getPacketType());

		assertEquals(28, rtcpSdes.getLength());

		RtcpSdesChunk rtcpSdesChunk = rtcpSdes.getSdesChunks()[0];

		assertNotNull(rtcpSdesChunk);

		assertEquals(266283887, rtcpSdesChunk.getSsrc());
		assertEquals(2, rtcpSdesChunk.getItemCount());

		RtcpSdesItem rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[0];

		assertNotNull(rtcpSdesItem);

		assertEquals(RtcpSdesItem.RTCP_SDES_CNAME, rtcpSdesItem.getType());
		assertEquals(14, rtcpSdesItem.getLength());
		assertEquals("QTSS1262189851", rtcpSdesItem.getText());

		rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[1];
		assertNotNull(rtcpSdesItem);
		assertEquals(RtcpSdesItem.RTCP_SDES_END, rtcpSdesItem.getType());

		// App Specific
		RtcpAppDefined rtcpAppDefined = rtcpPacket.getRtcpAppDefined();

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
	public void testDecode1() {
		// These values are from wireshark trace
		RtcpPacket rtcpPacket = new RtcpPacket();
		int length = rtcpPacket.decode(p1, 0);

		assertEquals(p1.length, length);

		// SDES
		RtcpSdes rtcpSdes = rtcpPacket.getRtcpSdes();
		assertEquals(2, rtcpSdes.getVersion());
		assertFalse(rtcpSdes.isPadding());
		assertEquals(1, rtcpSdes.getCount());

		assertEquals(RtcpCommonHeader.RTCP_SDES, rtcpSdes.getPacketType());

		assertEquals(32, rtcpSdes.getLength());

		RtcpSdesChunk rtcpSdesChunk = rtcpSdes.getSdesChunks()[0];

		assertNotNull(rtcpSdesChunk);

		assertEquals(3435894470l, rtcpSdesChunk.getSsrc());
		assertEquals(2, rtcpSdesChunk.getItemCount());

		RtcpSdesItem rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[0];

		assertNotNull(rtcpSdesItem);

		assertEquals(RtcpSdesItem.RTCP_SDES_CNAME, rtcpSdesItem.getType());
		assertEquals(18, rtcpSdesItem.getLength());
		assertEquals("abhayani@localhost", rtcpSdesItem.getText());

		rtcpSdesItem = rtcpSdesChunk.getRtcpSdesItems()[1];
		assertNotNull(rtcpSdesItem);
		assertEquals(RtcpSdesItem.RTCP_SDES_END, rtcpSdesItem.getType());

	}	

	@Test
	public void testEncode() {

		RtcpSenderReport rtcpSenderReport = new RtcpSenderReport(false, 266283887, 3471178652l, 133143986, 1586529211,
				1, 11);

		RtcpSdes rtcpSdes = new RtcpSdes(false);

		RtcpSdesItem rtcpSdesItem = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, "QTSS1262189851");

		RtcpSdesChunk rtcpSdesChunk = new RtcpSdesChunk(266283887);
		rtcpSdesChunk.addRtcpSdesItem(rtcpSdesItem);

		rtcpSdes.addRtcpSdesChunk(rtcpSdesChunk);

		byte[] appData = new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x61, 0x74, 0x00, 0x04, 0x00,
				0x00, 0x00, 0x14 };
		RtcpAppDefined rtcpAppDefined = new RtcpAppDefined(false, 1, 266283887, "qtsi", appData);

		RtcpPacket rtcpPacket = new RtcpPacket(rtcpSenderReport, null, rtcpSdes, null, rtcpAppDefined);

		byte[] rawData = new byte[256];

		int length = rtcpPacket.encode(rawData, 0);

		assertEquals(p.length, length);

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}

}
