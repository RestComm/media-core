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

package org.mobicents.media.server.impl.rtp.sdp;

import java.text.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class SessionDescriptionTest {

    private SessionDescription sd = new SessionDescription();
    private String sdp = "v=0\n" +
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

 private String linPhoneSDP = "v=0\n" +
            "o=blocked-sender 123456 654321 IN IP4 192.168.0.11\n" +
            "s=A conversation\n" +
            "c=IN IP4 192.168.0.11\n" +
            "t=0 0\n" +
            "m=audio 7078 RTP/AVP 111 110 0 8 101\n" +
            "c=IN IP4 192.168.0.11\n" +
            "a=rtpmap:111 speex/16000/1\n" +
            "a=rtpmap:110 speex/8000/1\n" +
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
    private String vladsSDP = "v=0\n" +
            "o=- 0 2 IN IP4 192.168.2.1\n" +
            "s=CounterPath X-Lite 3.0\n" +
            "c=IN IP4 192.168.2.1\n" +
            "t=0 0\n" +
            "m=audio 7688 RTP/AVP 0 8 101\n" +
            "a=fmtp:101 0-15\n" +
            "a=rtpmap:101 telephone-event/8000\n" +
            "a=alt:1 4 : CoF0Nv2H 5hS404hU 192.168.2.1 7688\n" +
            "a=alt:2 3 : sRdXzWqQ y5uUNWD2 192.168.1.2 7688\n" +
            "a=alt:3 2 : F6ddhsRx Ei6bATWC 10.211.55.2 7688\n" +
            "a=alt:4 1 : Fola+gz2 Pl1XgZb0 10.37.129.2 7688\n";

    private String sdp_template = "v=0\n" +
            "o=- 8 2 IN IP4 192.168.1.2\n" +
            "s=CounterPath X-Lite 3.0\n" +
            "c=IN IP4 192.168.1.2\n" +
            "t=0 0\n" +
            "m=audio ${audio.port} RTP/AVP 8 101\n" +
            "a=alt:1 1 : aZNEKdX5 FpbpFGUv 192.168.1.2 39958\n" +
            "a=fmtp:101 0-15\n" +
            "a=rtpmap:101 telephone-event/8000\n" +
            "a=sendrecv\n" +
            "m=video ${video.port} RTP/AVP 99 34 97 98 100\n" +
            "c=IN IP4 192.168.0.11\n" +
            "a=rtpmap:99 MP4V-ES/90000\n" +
            "a=fmtp:99 profile-level-id=3\n" +
            "a=rtpmap:34 H263/90000\n" +
            "a=rtpmap:97 theora/90000\n" +
            "a=rtpmap:98 H263-1998/90000\n" +
            "a=fmtp:98 CIF=1;QCIF=1\n" +
            "a=rtpmap:100 x-snow/90000\n";
    
    private String s;
    private volatile int a = 10;
    
    public SessionDescriptionTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ParseException {
        sd.parse(sdp.getBytes());
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of parse method, of class SessionDescription.
     */
    @Test
    public void testVersion() throws ParseException {
        assertEquals("0", sd.getVersion().toString());
    }

    @Test
    public void testOrigin() throws ParseException {
        assertEquals("192.168.1.2", sd.getOrigin().getAddress());
    }

    @Test
    public void testSession() throws ParseException {
        assertEquals("CounterPath X-Lite 3.0", sd.getSession());
    }

    @Test
    public void testConnection() throws ParseException {
        assertEquals("192.168.1.2", sd.getConnection().getAddress());
    }

    @Test
    public void testTime() throws ParseException {
        assertEquals(0, sd.getTime().getStart());
        assertEquals(0, sd.getTime().getStop());
    }
    
    @Test
    public void testParsingTime() throws ParseException {
        byte[] data = sdp.getBytes();
        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            sd.parse(data);
        }
        long duration = System.nanoTime() - s;
        System.out.println("Duration=" + duration);
    }

    @Test
    public void testSdpTemplate() {
        long s = System.nanoTime();
        String sdp = null;
        for (int i = 0; i < 1000; i++) {
            sdp = sdp_template.replace("${audio.port}", Integer.toString(1234));
            sdp = sdp.replace("${video.port}", Integer.toString(1234));
        }
        long duration = System.nanoTime() - s;
        System.out.println(duration);
        System.out.println(sdp);
    }

    @Test
    public void testSdpConstruction() {
        long st = System.nanoTime();
        
        sd.setVersion("0");
        sd.setOrigin("-", "1", "1", "IN", "IP4", "127.0.0.1");
        String s = sd.toString();

        long duration = System.nanoTime() - st;

        System.out.println(s);
        System.out.println("Duration= " + duration);
    }
    
    private int fetch1() {
        a = a + 1;
        return a;
    }

    private synchronized int fetch2() {
        a = a + 1;
        return a;
    }
    
    @Test
    public void testPerf() {
        long s = System.nanoTime();
        for (int i = 0; i < 10000000; i++) {
            fetch1();
        }
        
        long d1 = System.nanoTime() - s;
        
        s = System.nanoTime();
        for (int i = 0; i < 10000000; i++) {
            fetch2();
        }
        
        long d2 = System.nanoTime() - s;
        System.out.println("Performance relation: " + (double)d2/(double)d1);
         
    }
}