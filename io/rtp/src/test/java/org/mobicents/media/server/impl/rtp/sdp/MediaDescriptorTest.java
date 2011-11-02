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

package org.mobicents.media.server.impl.rtp.sdp;

import org.mobicents.media.server.utils.Text;
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
public class MediaDescriptorTest {

    private String m = "m=audio 39958 RTP/AVP 8 101\n" +
            "a=alt:1 1 : aZNEKdX5 FpbpFGUv 192.168.1.2 39958\n" +
            "a=rtpmap:101 telephone-event/8000\n" +
            "a=fmtp:101 0-15\n" +
            "a=sendrecv\n";
    
    private MediaDescriptorField md;
    private Text text = new Text();

    public MediaDescriptorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ParseException {
        byte[] data = m.getBytes();
        md = new MediaDescriptorField();

        text.strain(data,0, data.length);
        md.setDescriptor(text.nextLine());
        md.addAttribute(text.nextLine());
        md.addAttribute(text.nextLine());
        md.addAttribute(text.nextLine());
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testMediaType() {
        assertEquals("audio", md.getMediaType().toString());
    }

    @Test
    public void testProfile() {
        assertEquals("RTP/AVP", md.getProfile().toString());
    }
    /**
     * Test of getPort method, of class MediaDescriptor.
     */
    @Test
    public void testGetPort() {
        assertEquals(39958, md.getPort());
    }

    @Test
    public void testFormatAmount() {
        RTPFormats formats = md.getFormats();
        assertEquals(2, formats.size());
        
        assertTrue(formats.getRTPFormat(101).getFormat().getOptions() != null);
    }

    @Test
    public void testExecutionTime() throws ParseException {
        long s = System.nanoTime();
        byte[] data = m.getBytes();

        for (int i = 0; i < 1000; i++) {
            md = new MediaDescriptorField();
            text = new Text();
            text.strain(data,0, data.length);
            md.setDescriptor(text.nextLine());
            md.addAttribute(text.nextLine());
            md.addAttribute(text.nextLine());
            md.addAttribute(text.nextLine());
        }

        long f = System.nanoTime();
        System.out.println("Execution time: " + (f-s));
    }

}