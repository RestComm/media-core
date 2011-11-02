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

package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.tritonus.share.TDebug.AssertException;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class RtpPacketTest {

	//These values are from wireshark trace
	private byte[] p = new byte[] { (byte) 0x80, 0x08, 0x6a, 0x6c, (byte) 0xc1, (byte) 0xab, 0x74, (byte) 0x8d,
			(byte) 0xb2, (byte) 0xe2, (byte) 0x83, 0x69, 0x57, 0x57, 0x57, 0x57, 0x57, 0x57, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54, 0x54,
			0x54, 0x54, 0x54, 0x54, 0x54, 0x54 };

	public RtpPacketTest() {
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
	public void testRtpPacketHeaders() {
		//These values are from wireshark trace
		
		ByteBuffer buff = ByteBuffer.wrap(p);
		RtpPacket packet = new RtpPacket(buff);
		assertEquals(2, packet.getVersion());
		assertEquals(false, packet.isPadding());
		assertEquals(false, packet.isExtensions());
		assertEquals(0, packet.getContributingSource());
		assertEquals(false, packet.getMarker());
		assertEquals(8, packet.getPayloadType());
		assertEquals(27244, packet.getSeqNumber());
		assertEquals(3249239181l, packet.getTimestamp());
		assertEquals(3001189225l, packet.getSyncSource());
	}
	
	@Test
	public void testToByteArrayConversion() {
		ByteBuffer buff = ByteBuffer.wrap(p);
		RtpPacket packet = new RtpPacket(buff);
		
		byte[] rtpdata = packet.toByteArray();
		//ByteBuffer newBuff = ByteBuffer.wrap();
		
		assertEquals(p.length, rtpdata.length);
		
		int count= 0;
		for(byte b : p){
			assertEquals(b, rtpdata[count]);
			count ++;
		}
		
		
	}

}