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

package org.mobicents.media.server.impl.dsp.audio.speex;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.Buffer;
import org.mobicents.media.Frame;
import static org.junit.Assert.*;

/**
 *
 * @author Oleg Kulikov
 */
public class SpeexCodecTest {

    private final static byte[] ENCODED_SILENCE_NB_Q03_MONO = {30, -99, 102, 0, 0, 103, 57, -56, 16, 51, -100, -28, 8, 25, -50, 114, 4, 12, -25, 57};
    private final static byte[] silenceOriginal = new byte[320];
    private final static byte[] silence = new byte[320];
    
    public SpeexCodecTest() {
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
    
    @Test
    public void testCodec() {
        Buffer buffer = new Buffer();
        buffer.setData(silence);
        
        org.mobicents.media.server.spi.dsp.Codec compressor = new Encoder();
        compressor.process(buffer);
        
        byte[] res = buffer.getData();
        for (int i = 0; i < ENCODED_SILENCE_NB_Q03_MONO.length; i++) {
        	
            if (ENCODED_SILENCE_NB_Q03_MONO[i] != res[i]) {
                fail("mismatch found at " + i);
            }
        }
        org.mobicents.media.server.spi.dsp.Codec decompressor = new Decoder();
        decompressor.process(buffer);
      
        
        res = buffer.getData();
        for (int i = 0; i < silenceOriginal.length; i++) {
        	  	
            if (silenceOriginal[i] != res[i]) {
                fail("mismatch found at " + i);
            }
        }
    }

}