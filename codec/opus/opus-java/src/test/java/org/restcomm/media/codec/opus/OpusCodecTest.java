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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.codec.opus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.restcomm.media.codec.opus.Decoder;
import org.restcomm.media.codec.opus.Encoder;
import org.restcomm.media.codec.opus.OpusJni;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;


/**
 * Opus codec test class
 * 
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 * 
 */
public class OpusCodecTest {

    private static final Logger log = LogManager.getLogger(OpusCodecTest.class);

    private Frame buffer = Memory.allocate(512);
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    public OpusCodecTest() {        
    }            

    @Before
    public void setUp() throws Exception {
        buffer.setLength(512);         
    }

    @After
    public void tearDown() throws Exception {        
    }
    
    /**
     * Test of process method, for Encoder and Decoder.
     */
    @Test
    public void testCodec() throws Exception {
    	
        boolean testPassed = false;
    	
        try {
            final int packetSize = 480;
            File outputFile = File.createTempFile("opustest", ".tmp");
            URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48.pcm");
            Encoder encoder = new Encoder();
            Decoder decoder = new Decoder();
            try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile());
                FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {
                byte[] input = new byte[2 * packetSize];
                short[] inputData = new short[packetSize];
                while (inputStream.read(input) == 2 * packetSize) {
                     Frame inputFrame = Memory.allocate(2 * packetSize);
                     inputFrame.setOffset(0);
                     inputFrame.setLength(2 * packetSize);
                     inputFrame.setFormat(encoder.getSupportedInputFormat());
                     inputFrame.setTimestamp(System.currentTimeMillis());
                     inputFrame.setDuration(10);
                     System.arraycopy(input, 0, inputFrame.getData(), 0, 2 * packetSize);
                     Frame encodedFrame = encoder.process(inputFrame);
                     Frame decodedFrame = decoder.process(encodedFrame);
                     outputStream.write(decodedFrame.getData());
                }
                testPassed = true;
            } finally {
                outputFile.delete();
            }
	        
            outputFile.delete();
        } catch (IOException exc) {
            log.error("IOException: " + exc.getMessage());
            fail("Opus test file access error");
        }
    	
        assertTrue(testPassed);
    }
    
    /**
     * Test for observer.
     */
    @Test
    public void testObserver() throws Exception {
    	
        // given
    	  final OpusJni.Observer observer = mock(OpusJni.Observer.class);
    	
    	  // when
    	  OpusJni opus = new OpusJni();
    	  opus.setOpusObserverNative(observer);
    	  opus.sayHelloNative();
    	  opus.unsetOpusObserverNative();
    	
    	  // then
    	  verify(observer, times(1)).onHello();
    }
}
