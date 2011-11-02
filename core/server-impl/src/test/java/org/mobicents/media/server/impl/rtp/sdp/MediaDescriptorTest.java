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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.Format;
import org.mobicents.media.server.spi.MediaType;

/**
 *
 * @author kulikov
 */
public class MediaDescriptorTest {

    private String m = "m=audio 1234 RTP/AVP  0 101";
    private MediaDescriptor md;
    
    public MediaDescriptorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        md = new MediaDescriptor(m);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getMediaType method, of class MediaDescriptor.
     */
    @Test
    public void testGetMediaType() {
        assertEquals(MediaType.AUDIO, md.getMediaType());
    }

    /**
     * Test of getPort method, of class MediaDescriptor.
     */
    @Test
    public void testGetPort() {
        assertEquals(1234, md.getPort());
    }

    /**
     * Test of getProfle method, of class MediaDescriptor.
     */
    @Test
    public void testGetProfle() {
        assertEquals("RTP/AVP", md.getProfle());
    }

    /**
     * Test of getMode method, of class MediaDescriptor.
     */
    @Test
    public void testGetMode() {
        assertEquals(null, md.getMode());
    }

    /**
     * Test of getFormatCount method, of class MediaDescriptor.
     */
    @Test
    public void testGetFormatCount() {
        assertEquals(2, md.getFormatCount());
    }

    /**
     * Test of getPyaloadType method, of class MediaDescriptor.
     */
    @Test
    public void testGetPyaloadType() {
        assertEquals(0, md.getPyaloadType(0));
        assertEquals(101, md.getPyaloadType(1));
    }


}