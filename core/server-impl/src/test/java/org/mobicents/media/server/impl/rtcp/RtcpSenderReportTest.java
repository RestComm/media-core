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
public class RtcpSenderReportTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x80, (byte) 0xc8, 0x00, 0x06, 0x0f, (byte) 0xdf, 0x2b, 0x6f, (byte) 0xce,
			(byte) 0xe5, (byte) 0xfb, (byte) 0x9c, 0x07, (byte) 0xef, (byte) 0x9d, (byte) 0xb2, 0x5e, (byte) 0x90,
			(byte) 0x83, (byte) 0xbb, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x0b };

	public RtcpSenderReportTest() {
		// TODO Auto-generated constructor stub
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

	@Test
	public void testDecode() {
		RtcpSenderReport rtcpSenderReport = new RtcpSenderReport();

		rtcpSenderReport.decode(p, 0);

		assertEquals(2, rtcpSenderReport.getVersion());
		assertFalse(rtcpSenderReport.isPadding());
		assertEquals(0, rtcpSenderReport.getCount());

		assertEquals(RtcpCommonHeader.RTCP_SR, rtcpSenderReport.getPt());

		assertEquals(28, rtcpSenderReport.getLength());

		assertEquals(266283887, rtcpSenderReport.getSsrc());

		assertEquals(3471178652l, rtcpSenderReport.getNtpSec());
		assertEquals(133143986, rtcpSenderReport.getNtpFrac());

		assertEquals(1586529211, rtcpSenderReport.getRtpTs());

		assertEquals(1, rtcpSenderReport.getPsent());

		assertEquals(11, rtcpSenderReport.getOsent());

	}

	@Test
	public void testEncode() {

		RtcpSenderReport rtcpSenderReport = new RtcpSenderReport(false, 266283887, 3471178652l, 133143986, 1586529211,
				1, 11);

		byte[] rawData = new byte[256];

		int length = rtcpSenderReport.encode(rawData, 0);

		assertEquals(p.length, length);

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}
}
