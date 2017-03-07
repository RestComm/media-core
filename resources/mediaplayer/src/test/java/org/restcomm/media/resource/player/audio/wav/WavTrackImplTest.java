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

package org.restcomm.media.resource.player.audio.wav;

import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.resource.player.audio.wav.WavTrackImpl;

import static org.junit.Assert.*;
/**
 *
 * @author kulikov
 */
public class WavTrackImplTest {

    private WavTrackImpl track;
    
    public WavTrackImplTest() {
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
     * Test of setPeriod method, of class WavTrackImpl.
     */
    @Test
    public void testDurationd() throws Exception {
        /*URL url = WavTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/addf8-Alaw-GW.wav");
        URL url1 = WavTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/8kulaw.wav");
        
        track = new WavTrackImpl(url);
        assertEquals(2976000000L, track.getDuration());
        
        long s = System.nanoTime();
        track = new WavTrackImpl(url1);
        
        long f = System.nanoTime();
        System.out.println(f-s);*/
    }


}