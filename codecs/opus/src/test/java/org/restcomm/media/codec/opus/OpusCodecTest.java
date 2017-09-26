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

package org.restcomm.media.codec.opus;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.codec.opus.Decoder;
import org.restcomm.media.codec.opus.Encoder;
import org.restcomm.media.codec.opus.OpusJni;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

public class OpusCodecTest implements OpusJni.Observer {
    private Frame buffer = Memory.allocate(512);
    
    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    private byte[] src = new byte[512];
    
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
    public void testCodec() {
    	OpusJni opus = new OpusJni();
    	opus.setOpusObserverNative(this);
    	opus.sayHelloNative();
    	
        org.restcomm.media.spi.dsp.Codec compressor = new Encoder();
        long s = System.nanoTime();
        compressor.process(buffer);
        long f = System.nanoTime();
        System.out.println("Duration=" + (f-s));
        
        org.restcomm.media.spi.dsp.Codec decompressor = new Decoder();
        s = System.nanoTime();
        decompressor.process(buffer);
        f = System.nanoTime();
        System.out.println("Duration=" + (f-s));
    }
    
    @Override
    public void onHello() {
    	System.out.println("Hello World - Java!");
    }
}
