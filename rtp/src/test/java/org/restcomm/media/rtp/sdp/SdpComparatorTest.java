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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.media.rtp.sdp;

import java.text.ParseException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.core.sdp.format.RTPFormats;
import org.restcomm.media.rtp.sdp.SdpComparator;
import org.restcomm.media.rtp.sdp.SessionDescription;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
@Deprecated
public class SdpComparatorTest {

    private SessionDescription sd1 = new SessionDescription();
    private SessionDescription sd2 = new SessionDescription();
    private SdpComparator comparator = new SdpComparator();

    private String sdp1 = "v=0\n" +
            "o=- 8 2 IN IP4 192.168.1.2\n" +
            "s=CounterPath X-Lite 3.0\n" +
            "c=IN IP4 192.168.1.2\n" +
            "t=0 0\n" +
            "m=audio 39958 RTP/AVP 8 101\n" +
            "a=alt:1 1 : aZNEKdX5 FpbpFGUv 192.168.1.2 39958\n" +
            "a=fmtp:101 0-15\n" +
            "a=rtpmap:101 telephone-event/8000\n" +
            "a=sendrecv\n" +
            "m=video 9078 RTP/AVP 99 34 97 98 100\n" +
            "c=IN IP4 192.168.0.11\n" +
            "a=rtpmap:99 MP4V-ES/90000\n" +
            "a=fmtp:99 profile-level-id=3\n" +
            "a=rtpmap:34 H263/90000\n" +
            "a=rtpmap:97 theora/90000\n" +
            "a=rtpmap:98 H263-1998/90000\n" +
            "a=fmtp:98 CIF=1;QCIF=1\n" +
            "a=rtpmap:100 x-snow/90000\n";

     private String sdp2 = "v=0\n" +
            "o=blocked-sender 123456 654321 IN IP4 192.168.0.11\n" +
            "s=A conversation\n" +
            "c=IN IP4 192.168.0.11\n" +
            "t=0 0\n" +
            "m=audio 7078 RTP/AVP 111 110 0 8 101\n" +
            "c=IN IP4 192.168.0.11\n" +
            "a=rtpmap:0 PCMU/8000/1\n" +
            "a=rtpmap:8 PCMA/8000/1\n" +
            "a=rtpmap:101 telephone-event/8000/1\n" +
            "a=fmtp:101 0-11\n" +
            "a=sendrecv\n" +
            "m=video 9078 RTP/AVP 99 34 97 98 100\n" +
            "c=IN IP4 192.168.0.11\n" +
            "a=rtpmap:99 MP4V-ES/90000\n" +
            "a=fmtp:99 profile-level-id=3\n" +
            "a=rtpmap:34 H263/90000\n" +
            "a=rtpmap:97 theora/90000\n" +
            "a=rtpmap:98 H263-1998/90000\n" +
            "a=fmtp:98 CIF=1;QCIF=1\n" +
            "a=rtpmap:100 x-snow/90000\n";

    public SdpComparatorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ParseException {
        sd1.parse(sdp1.getBytes());
        sd2.parse(sdp2.getBytes());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of compare method, of class SdpComparator.
     */
    @Test
    public void testAudio() {
    	comparator.compare(sd1, sd2);

        RTPFormats fmts = comparator.getAudio();
        assertEquals(2, fmts.size());    	
    }

    @Test
    public void testAudioTime() {
        long s = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            comparator.compare(sd1, sd2);
        }
        long duration = System.nanoTime() - s;
        System.out.println("Duration " + duration);
    }

}
