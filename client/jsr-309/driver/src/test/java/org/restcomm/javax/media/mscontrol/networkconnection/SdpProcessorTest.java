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
package org.restcomm.javax.media.mscontrol.networkconnection;

import javax.sdp.MediaDescription;
import java.util.Vector;
import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.javax.media.mscontrol.networkconnection.SdpProcessor;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class SdpProcessorTest {

    private String s = "v=0\n"
            + "o=- 1815849 0 IN IP4 194.67.15.181\n"
            + "s=Cisco SDP 0\n"
            + "c=IN IP4 194.67.15.181\n"
            + "t=0 0\n"
            + "m=audio 20062 RTP/AVP 99 18 101 100\n"
            + "a=rtpmap:99 G.729b/8000\n"
            + "a=rtpmap:101 telephone-event/8000\n"
            + "a=fmtp:101 0-15\n"
            + "a=rtpmap:100 X-NSE/8000\n"
            + "a=fmtp:100 200-202";
    private String s1 = "v=0\n"
            + "o=- 1815849 0 IN IP4 194.67.15.181\n"
            + "s=Cisco SDP 0\n"
            + "c=IN IP4 194.67.15.181\n"
            + "t=0 0\n"
            + "m=audio 42412 RTP/AVP 0 8 4 18 97 99 101 100\n"
            + "c=IN IP4 16.16.93.241\n"
            + "b=TIAS:64000\n"
            + "b=AS:80\n"
            + "a=rtpmap:0 PCMU/8000/1\n"
            + "a=rtpmap:8 PCMA/8000/1\n"
            + "a=rtpmap:4 G723/8000/1\n"
            + "a=fmtp:4 bitrate=5.3,6.3;annexb=yes\n"
            + "a=rtpmap:18 G729/8000/1\n"
            + "a=fmtp:18 annexb=yes\n"
            + "a=rtpmap:97 EVRC0/8000/1\n"
            + "a=rtpmap:99 AMR/8000/1\n"
            + "a=fmtp:99 mode-set=7;octet-align=1\n"
            + "a=rtpmap:101 telephone-event/8000/1\n"
            + "a=rtpmap:100 AMR-WB/16000/1\n"
            + "a=fmtp:100 mode-set=8;octet-align=1\n"
            + "a=sendrecv";
    
    private final static String s2 = "v=0\n"
            + "o=HP-OCMP 756 2 IN IP4 16.16.93.241\n"
            + "s=-\n"
            + "c=IN IP4 16.16.93.241\n"
            + "t=0 0\n"
            + "m=audio 42412 RTP/AVP 0";
    
    
    private SdpProcessor sdpProcessor = new SdpProcessor();
    private SdpFactory sdpFactory = SdpFactory.getInstance();
    
    
    private SessionDescription sdp;

    public SdpProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws SdpParseException {
        sdp = sdpFactory.createSessionDescription(s);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of containsFormat method, of class SdpProcessor.
     */
    @Test
    public void testContainsFormat() throws Exception {
        assertTrue("Excepted format G.729b", sdpProcessor.containsFormat("G.729b", sdp));
        assertTrue("Excepted format G.729b", sdpProcessor.containsFormat("g.729b", sdp));
    }

    /**
     * Test of containsMedia method, of class SdpProcessor.
     */
    @Test
    public void testContainsMedia() throws Exception {
        assertTrue("Excepted audio descriptor", sdpProcessor.containsMedia("audio", sdp));
        assertTrue("Excepted audio descriptor", sdpProcessor.containsMedia("AUDIO", sdp));
    }

    /**
     * Test of exclude method, of class SdpProcessor.
     */
    @Test
    public void testExclude() throws Exception {
        sdpProcessor.exclude("G.729b", sdp);
        assertFalse("SDP should not contain G.729b", sdpProcessor.containsFormat("G.729b", sdp));
    }
    
    @Test
    public void testExclude2() throws Exception {
        SessionDescription sdp1 = sdpFactory.createSessionDescription(s1);
        sdpProcessor.exclude("telephone-event", sdp1);
        System.out.println(sdp1);
        assertFalse("SDP should not contain telephone-event", sdpProcessor.containsFormat("telephone-event", sdp1));
    }

    @Test
    public void testExclude3() throws Exception {
        SessionDescription sdp1 = sdpFactory.createSessionDescription(s2);
        sdpProcessor.exclude("PCMU", sdp1);
        System.out.println(sdp1);
        assertFalse("SDP should not contain pcmu", sdpProcessor.containsFormat("PCMU", sdp1));
        
        Vector<MediaDescription> mds = sdp1.getMediaDescriptions(false);
        assertTrue("Format definition", mds.get(0).getMedia().getMediaFormats(false) == null);
    }
    
    
    @Test
    public void testMinimalOffer() throws Exception  {
        assertTrue("Minimal offer", sdpProcessor.checkForMinimalOffer(sdp));
    }
    
}
