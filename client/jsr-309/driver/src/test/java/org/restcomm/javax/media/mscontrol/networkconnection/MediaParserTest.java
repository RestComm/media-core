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

package org.restcomm.javax.media.mscontrol.networkconnection;

import java.util.Iterator;
import java.util.Vector;

import javax.sdp.MediaDescription;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.javax.media.mscontrol.networkconnection.MediaParser;
import org.restcomm.sdp.AVProfile;

public class MediaParserTest {

	public static final String richOffer = "v=0\r\n" + "o=HP-OCMP 756 2 IN IP4 16.16.93.241\r\n" + "s=-\r\n"
			+ "c=IN IP4 16.16.93.241\r\n" + "t=0 0\r\n" + "m=audio 42412 RTP/AVP 0 8 4 18 97 99 101 100\r\n"
			+ "c=IN IP4 16.16.93.241\r\n" + "b=TIAS:64000\r\n" + "b=AS:80\r\n" + "a=rtpmap:0 PCMU/8000/1\r\n"
			+ "a=rtpmap:8 PCMA/8000/1\r\n" + "a=rtpmap:4 G723/8000/1\r\n" + "a=fmtp:4 bitrate=5.3,6.3;annexb=yes\r\n"
			+ "a=rtpmap:18 G729/8000/1\r\n" + "a=fmtp:18 annexb=yes\r\n" + "a=rtpmap:97 EVRC0/8000/1\r\n"
			+ "a=rtpmap:99 AMR/8000/1\r\n" + "a=fmtp:99 mode-set=7;octet-align=1\r\n"
			+ "a=rtpmap:101 telephone-event/8000/1\r\n" + "a=rtpmap:100 AMR-WB/16000/1\r\n"
			+ "a=fmtp:100 mode-set=8;octet-align=1\r\n" + "a=sendrecv\r\n";
	private static final SdpFactory sdpFactory = SdpFactory.getInstance();

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
	public void testExcludes() throws Exception {
		
		System.out.println(richOffer);
		
		System.out.println("--------------");

		String[] excludes = new String[] { "G723","G729","EVRC0","AMR-WB",
				"telephone-event","PCMA","PCMU" };

		SessionDescription sessiondescription = sdpFactory.createSessionDescription(richOffer);
		Vector v = sessiondescription.getMediaDescriptions(false);

		Iterator itr = v.iterator();
		while (itr.hasNext()) {
			MediaDescription m = (MediaDescription) itr.next();
			MediaParser.excludeCodec(m, excludes);
		}
		
		
		System.out.println(sessiondescription);
		
		System.out.println(AVProfile.G729.encode());
	}
}
