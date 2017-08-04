/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.mscontrol.sdp;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.mscontrol.sdp.AVProfile;
import org.restcomm.mscontrol.sdp.MediaType;
import org.restcomm.mscontrol.sdp.SessionDescriptor;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class SessionDescriptorTest {

    public SessionDescriptorTest() {
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
     * Test of toString method, of class SessionDescriptor.
     */
    @Test
    public void testExclude() {
        String sdp = "v=0\n" +
                "o=- 16410075 16410075 IN IP4 127.0.0.1\n" +
                "s=Mobicents Media Server\n" +
                "c=IN IP4 127.0.0.1\n" +
                "t=0 0\n" +
                "m=audio 1186 RTP/AVP 0 101\n" +
                "a=rtpmap:0 pcmu/8000\n" +
                "a=rtpmap:101 telephone-event/8000\n" +
                "a=fmtp:101 0-15\n" +
                "a=control:audio\n" +
                "a=silenceSupp:off\n";

        SessionDescriptor sd = new SessionDescriptor(sdp);
        sd.exclude(MediaType.AUDIO, AVProfile.DTMF);

        String s = sd.toString();
        System.out.println(s);
        assertEquals(-1, s.indexOf("101"));
        assertEquals(-1, s.indexOf("telephone-event"));
    }

    /**
     * Test of toString method, of class SessionDescriptor.
     */
    @Test
    public void testExclude1() {
        String sdp = "v=0\n" +
                "o=- 16410075 16410075 IN IP4 127.0.0.1\n" +
                "s=Mobicents Media Server\n" +
                "c=IN IP4 127.0.0.1\n" +
                "t=0 0\n" +
                "m=audio 1186 RTP/AVP 0 101\n" +
                "a=rtpmap:0 pcmu/8000\n" +
                "a=rtpmap:101 telephone-event/8000\n" +
                "a=fmtp:101 0-15\n" +
                "a=control:audio\n" +
                "a=silenceSupp:off\n";

        SessionDescriptor sd = new SessionDescriptor(sdp);
        sd.exclude("telephone-Event");

        String s = sd.toString();
        System.out.println(s);
        assertEquals(-1, s.indexOf("101"));
        assertEquals(-1, s.indexOf("telephone-event"));
    }

    @Test
    public void testExclude2() {
        String sdp = "v=0\n" +
                "o=HP-OCMP 756 2 IN IP4 16.16.93.241\n" +
                "s=-\n" +
                "c=IN IP4 16.16.93.241\n" +
                "t=0 0\n" +
                "m=audio 42412 RTP/AVP 0 8 4 18 97 99 101 100\n" +
                "c=IN IP4 16.16.93.241\n" +
                "b=TIAS:64000\n" +
                "b=AS:80\n" +
                "a=rtpmap:0 PCMU/8000/1\n" +
                "a=rtpmap:8 PCMA/8000/1\n" +
                "a=rtpmap:4 G723/8000/1\n" +
                "a=fmtp:4 bitrate=5.3,6.3;annexb=yes\n" +
                "a=rtpmap:18 G729/8000/1\n" +
                "a=fmtp:18 annexb=yes\n" +
                "a=rtpmap:97 EVRC0/8000/1\n" +
                "a=rtpmap:99 AMR/8000/1\n" +
                "a=fmtp:99 mode-set=7;octet-align=1\n" +
                "a=rtpmap:101 telephone-event/8000/1\n" +
                "a=rtpmap:100 AMR-WB/16000/1\n" +
                "a=fmtp:100 mode-set=8;octet-align=1\n" +
                "a=sendrecv\n";


        SessionDescriptor sd = new SessionDescriptor(sdp);
        sd.exclude("pcma");

        String s = sd.toString();
        System.out.println(s);
        assertEquals(-1, s.indexOf("pcma"));

    }

    @Test
    public void testExclude3() {
        String sdp = "v=0\n" +
                "o=HP-OCMP 756 2 IN IP4 16.16.93.241\n" +
                "s=-\n" +
                "c=IN IP4 16.16.93.241\n" +
                "t=0 0\n" +
                "m=audio 42412 RTP/AVP 0 8 4 18 97 99 101 100\n" +
                "c=IN IP4 16.16.93.241\n" +
                "b=TIAS:64000\n" +
                "b=AS:80\n" +
                "a=rtpmap:0 PCMU/8000/1\n" +
                "a=rtpmap:8 PCMA/8000/1\n" +
                "a=rtpmap:4 G723/8000/1\n" +
                "a=fmtp:4 bitrate=5.3,6.3;annexb=yes\n" +
                "a=rtpmap:18 G729/8000/1\n" +
                "a=fmtp:18 annexb=yes\n" +
                "a=rtpmap:97 EVRC0/8000/1\n" +
                "a=rtpmap:99 AMR/8000/1\n" +
                "a=fmtp:99 mode-set=7;octet-align=1\n" +
                "a=rtpmap:101 telephone-event/8000/1\n" +
                "a=rtpmap:100 AMR-WB/16000/1\n" +
                "a=fmtp:100 mode-set=8;octet-align=1\n" +
                "a=sendrecv\n";


        SessionDescriptor sd = new SessionDescriptor(sdp);
        sd.exclude("fmtp");
        sd.exclude("AS");
        sd.exclude("sendrecv");
        sd.exclude("audio");
        sd.exclude("IP4");
        sd.exclude("G722");
        
        String s = sd.toString();
        System.out.println("====================================================");
        System.out.println(s);
    }
}
