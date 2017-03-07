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

package org.restcomm.media.component.dsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.restcomm.media.codec.g711.alaw.Decoder;
import org.restcomm.media.codec.g711.alaw.Encoder;
import org.restcomm.media.spi.format.Format;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class DspFactoryTest {

    private DspFactoryImpl dspFactory = new DspFactoryImpl();

    /**
     * Test of addCodec method, of class DspFactory.
     */
    @Test
    public void testLoading() throws Exception {
        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());
        dspFactory.newProcessor();
    }

    @Test
    public void testUnknownInput() throws Exception {
        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());

        Dsp dsp = dspFactory.newProcessor();

        Frame frame = Memory.allocate(320);
        Frame frame2 = dsp.process(frame,null,null);

        assertEquals(frame, frame2);
    }
    
    @Test
    public void testUndefinedOutput() throws Exception {
        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());

        Dsp dsp = dspFactory.newProcessor();

        Frame frame = Memory.allocate(320);
        frame.setFormat(FormatFactory.createAudioFormat("test", 8000));

        Frame frame2 = dsp.process(frame,frame.getFormat(),null);

        assertEquals(frame, frame2);
    }

    @Test
    public void testInputToOutputMatch() throws Exception {
        Format fmt = FormatFactory.createAudioFormat("test", 8000);
        
        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());

        Dsp dsp = dspFactory.newProcessor();
        
        Frame frame = Memory.allocate(320);
        frame.setFormat(fmt);

        Frame frame2 = dsp.process(frame,fmt,null);

        assertEquals(frame, frame2);
    }

    @Test
    public void testEncoding() throws Exception {
    	Format fmt = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
        Format fmt2 = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);

        dspFactory.addCodec(Encoder.class.getName());
        dspFactory.addCodec(Decoder.class.getName());

        Dsp dsp = dspFactory.newProcessor();
        
        Frame frame = Memory.allocate(320);
        frame.setFormat(fmt);

        Frame frame2 = dsp.process(frame,fmt,fmt2);

        System.out.println("fmt=" + frame2.getFormat().getName());
        assertTrue("Format missmatch", fmt2.matches(frame2.getFormat()));    	
    }
}