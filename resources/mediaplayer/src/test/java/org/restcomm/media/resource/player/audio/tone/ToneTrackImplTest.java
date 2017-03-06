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

package org.restcomm.media.resource.player.audio.tone;

import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.resource.player.audio.tone.ToneTrackImpl;

import static org.junit.Assert.*;
/**
 *
 * @author oifa yulian
 */
public class ToneTrackImplTest {

    private ToneTrackImpl track,track1,track2;
    
    public ToneTrackImplTest() {
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
    	try
    	{
    		URL url = new URL("file://5.tone");
    		URL url2 = new URL("file://*.tone");
    		URL url3 = new URL("file://B.tone");
    	
    		track = new ToneTrackImpl(url);
    		track1 = new ToneTrackImpl(url2);
    		track2 = new ToneTrackImpl(url3);
        
    		assertEquals(100000000L, track.getDuration());
    		assertEquals(100000000L, track1.getDuration());
    		assertEquals(100000000L, track2.getDuration());    	
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
        /*URL url = WavTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/addf8-Alaw-GW.wav");
        URL url1 = WavTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/8kulaw.wav");
        
        track = new WavTrackImpl(url);
        assertEquals(100000000L, track.getDuration());
        
        long s = System.nanoTime();
        track = new WavTrackImpl(url1);
        
        long f = System.nanoTime();
        System.out.println(f-s);*/
    }


}