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
import org.restcomm.media.core.rtcp.RtcpReceiverReport;
import org.restcomm.media.core.rtcp.RtcpReportBlock;

/**
 * 
 * @author amit bhayani
 * 
 */
public class RtcpReceiverReportTest {

	// These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x81, (byte) 0xc9, 0x00, 0x07, 0x0d, (byte) 0xe9, 0x4f, 0x1f, 0x0f,
			(byte) 0xdf, 0x2b, 0x6f, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x43, 0x62, 0x00, 0x00, 0x00, 0x39,
			(byte) 0xfb, (byte) 0x9c, 0x07, (byte) 0xef, 0x00, 0x04, 0x06, (byte) 0xab };

	public RtcpReceiverReportTest() {
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
		RtcpReceiverReport rtcpReceptionReport = new RtcpReceiverReport();

		rtcpReceptionReport.decode(p, 0);

		assertEquals(2, rtcpReceptionReport.getVersion());
		assertFalse(rtcpReceptionReport.isPadding());
		assertEquals(1, rtcpReceptionReport.getCount());

		assertEquals(RtcpHeader.RTCP_RR, rtcpReceptionReport.getPacketType());

		assertEquals(32, rtcpReceptionReport.getLength());

		assertEquals(233393951, rtcpReceptionReport.getSsrc());

		RtcpReportBlock rtcpReceptionReportItem = rtcpReceptionReport.getReportBlocks()[0];

		assertNotNull(rtcpReceptionReportItem);

		assertEquals(266283887, rtcpReceptionReportItem.getSsrc());
		assertEquals(0, rtcpReceptionReportItem.getFraction());
		assertEquals(0, rtcpReceptionReportItem.getLost());
		assertEquals(0, rtcpReceptionReportItem.getSeqNumCycle());
		assertEquals(17250, rtcpReceptionReportItem.getLastSeq());
		assertEquals(57, rtcpReceptionReportItem.getJitter());
		assertEquals(4221306863l, rtcpReceptionReportItem.getLsr());
		assertEquals(263851, rtcpReceptionReportItem.getDlsr());

	}

	@Test
	public void testEncode() {

		RtcpReceiverReport rtcpReceptionReport = new RtcpReceiverReport(false, 233393951);

		RtcpReportBlock rtcpReceptionReportItem = new RtcpReportBlock(266283887, 0, 0, 0, 17250, 57,
				4221306863l, 263851);
		
		rtcpReceptionReport.addReceiverReport(rtcpReceptionReportItem);

		byte[] rawData = new byte[256];

		int length = rtcpReceptionReport.encode(rawData, 0);

		assertEquals(p.length, length);
		assertEquals(1, rtcpReceptionReport.getCount());

		for (int i = 0; i < p.length; i++) {
			assertEquals(p[i], rawData[i]);
		}
	}

}
