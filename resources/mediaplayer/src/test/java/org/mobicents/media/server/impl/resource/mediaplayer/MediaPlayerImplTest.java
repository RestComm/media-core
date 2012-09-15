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

package org.mobicents.media.server.impl.resource.mediaplayer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.resource.Player;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;

/**
 *
 * @author yulian oifa
 */
public class MediaPlayerImplTest {
	//
    private AudioPlayerImpl audioPlayer;
    
    private Scheduler scheduler;
    public MediaPlayerImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {    	
    	scheduler = new Scheduler();
    	scheduler.setClock(new DefaultClock());
        scheduler.start();
        
        audioPlayer = new AudioPlayerImpl("test", scheduler);
    }

    @After
    public void tearDown() {    	
//        server.stop();
    	scheduler.stop();    	
    	audioPlayer = null;
    	
    }

    /**
     * Test of getMediaTypes method, of class MediaPlayerImpl.
     */
    @Test
    public void testAudio() throws Exception {    	
    }

    /**
     * Test of getMediaTypes method, of class MediaPlayerImpl.
     */
//    @Test
//    public void testVideo() {        
//    }
}
