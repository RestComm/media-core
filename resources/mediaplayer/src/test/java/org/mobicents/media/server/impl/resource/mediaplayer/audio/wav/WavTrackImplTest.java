// /*
// * JBoss, Home of Professional Open Source
// * Copyright 2011, Red Hat, Inc. and individual contributors
// * by the @authors tag. See the copyright.txt in the distribution for a
// * full listing of individual contributors.
// *
// * This is free software; you can redistribute it and/or modify it
// * under the terms of the GNU Lesser General Public License as
// * published by the Free Software Foundation; either version 2.1 of
// * the License, or (at your option) any later version.
// *
// * This software is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// * Lesser General Public License for more details.
// *
// * You should have received a copy of the GNU Lesser General Public
// * License along with this software; if not, write to the Free
// * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
// * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
// */
//
//package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;
//
//import java.io.IOException;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.TimeUnit;
//import javax.sound.sampled.UnsupportedAudioFileException;
//import org.apache.log4j.Logger;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import static org.junit.Assert.*;
//import org.mobicents.media.server.spi.format.EncodingName;
//
///**
// *
// * @author apollo
// */
//public class WavTrackImplTest {
//
//    private WavTrackImpl wavTrackPcm;
//    private WavTrackImpl wavTrackUlaw;
//    private WavTrackImpl wavTrackAlaw;
//    private URL url_pcm;
//    private URL url_ulaw;
//    private URL url_alaw;
//    
//    private static Logger logger = Logger.getLogger(WavTrackImplTest.class);
//    
//    public WavTrackImplTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//    }
//
//    @Before
//    public void setUp() throws IOException, UnsupportedAudioFileException {
//        url_pcm = WavTrackImplTest.class.getClassLoader().getResource("welcome.wav");
//        url_ulaw = WavTrackImplTest.class.getClassLoader().getResource("welcome_ulaw.wav");
//        url_alaw = WavTrackImplTest.class.getClassLoader().getResource("welcome_alaw.wav");
//        wavTrackPcm = new WavTrackImpl(url_pcm);
//        wavTrackUlaw = new WavTrackImpl(url_ulaw);
//        wavTrackAlaw = new WavTrackImpl(url_alaw);
//    }
//
//    @After
//    public void tearDown() {
//        wavTrackPcm.close();
//        wavTrackUlaw.close();
//        wavTrackAlaw.close();
//    }
//
//    /**
//     * Test of setPeriod method, of class WavTrackImpl.
//     */
//    @Test
//    public void testDurationd() throws Exception {
//        /*
//        wavTrackPcm = new WavTrackImpl(url_pcm); 
//        wavTrackPcm.process(0);
//        byte[] expectedByteArray = new byte["RIFF".getBytes().length]; 
//        
//        URL url = WavTrackImplTest.class.getClassLoader().getResource(
//                "org/mobicents/media/server/impl/addf8-Alaw-GW.wav");
//        URL url1 = WavTrackImplTest.class.getClassLoader().getResource(
//                "org/mobicents/media/server/impl/8kulaw.wav");
//        
//        assertEquals(2976000000L, wavTrackPcm.getDuration());
//        long s = System.nanoTime();
//        wavTrackPcm = new WavTrackImpl(url1);
//        long f = System.nanoTime();
//        System.out.println(f-s);*/
//    }
//    
//    /* Testing format for 3 wav files with different encodings: pcm, ulaw, alaw */
//    
//    @Test
//    public void testFormatName() throws Exception {
//        
//        wavTrackPcm.process(0);
//        EncodingName formatName = wavTrackPcm.getFormat().getName();
//        logger.info("Format name: " + formatName);
//        assertEquals("linear" , formatName.toString());
//     
//        wavTrackUlaw.process(0);
//        formatName = wavTrackUlaw.getFormat().getName();
//        logger.info("Format name: " + formatName);
//        assertEquals("pcmu" , formatName.toString());
//        
//        wavTrackAlaw.process(0);
//        formatName = wavTrackAlaw.getFormat().getName();
//        logger.info("Format name: " + formatName);
//        assertEquals("pcma" , formatName.toString());
//        
//    }
//    
//    /* Testing total read bytes from channel for 3 wav files with different encodings */
//   
//    @Test
//    public void testTotalRead() throws Exception {
//        
//        wavTrackPcm.process(0);
//        logger.info("TotalRead: " + wavTrackPcm.totalRead);
//        assertNotNull(wavTrackPcm.totalRead);
//        assertEquals(wavTrackPcm.totalRead , wavTrackPcm.frameSize);
//        
//        wavTrackUlaw.process(0);
//        logger.info("TotalRead: " + wavTrackUlaw.totalRead);
//        assertNotNull(wavTrackUlaw.totalRead);
//        assertEquals(wavTrackUlaw.totalRead , wavTrackUlaw.frameSize);
//        
//        wavTrackAlaw.process(0);
//        logger.info("TotalRead: " + wavTrackAlaw.totalRead);
//        assertNotNull(wavTrackAlaw.totalRead);
//        assertEquals(wavTrackAlaw.totalRead , wavTrackAlaw.frameSize);
//        
//    }
//    
//    /* Testing skip(timestamp) in case timestamp is greater than 0. */
//    
//    @Test
//    public void testSkip() throws Exception {
//        wavTrackPcm.process(40000000L);
//        logger.info("Total read from skip: " + wavTrackPcm.totalRead);
//        assertEquals(wavTrackPcm.totalRead , wavTrackPcm.frameSize);
//        
//        wavTrackUlaw.process(30000000L);
//        logger.info("Total read from skip: " + wavTrackUlaw.totalRead);
//        assertEquals(wavTrackUlaw.totalRead , wavTrackUlaw.frameSize);
//        
//        wavTrackAlaw.process(20000000L);
//        logger.info("Total read from skip: " + wavTrackAlaw.totalRead);
//        assertEquals(wavTrackAlaw.totalRead , wavTrackAlaw.frameSize);
//        
//    }
//
//}