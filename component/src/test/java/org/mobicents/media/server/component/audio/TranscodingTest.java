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

package org.mobicents.media.server.component.audio;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.io.Pipe;

/**
 *
 * @author kulikov
 */
public class TranscodingTest {

    private DspFactoryImpl dspFactory = new DspFactoryImpl();
    private Formats srcFormats = new Formats();
    private Formats dstFormats = new Formats();

    private Clock clock;
    private Scheduler scheduler;

    private Sine sine;
    private SpectraAnalyzer analyzer;
    
    private Dsp dsp1, dsp2;

    private short A = Short.MAX_VALUE / 4;
    private int f = 50;
    
    public TranscodingTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        srcFormats.add(FormatFactory.createAudioFormat("pcma", 8000, 8, 1));
        dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

        dsp1 = dspFactory.newProcessor();
        dsp2 = dspFactory.newProcessor();

        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        analyzer = new SpectraAnalyzer("analyzer", scheduler);

        Pipe pipe = new PipeImpl();

        pipe.connect(sine);
        pipe.connect(analyzer);

        sine.setDsp(dsp1);
        analyzer.setDsp(dsp2);

        sine.setFormats(srcFormats);
        analyzer.setFormats(dstFormats);
        
        sine.setFrequency(50);
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    @Test
    public void testSetFormats() {
        System.out.println("****************************");
        Formats fmts = sine.getFormats();
        assertEquals(2, fmts.size());
//        assertTrue(fmts.get(0).matches((FormatFactory.createAudioFormat("pcma", 8000, 8, 1))));
    }

    /**
     * Test of setAmplitude method, of class Sine.
     */
    @Test
    public void testSignal() throws Exception {
        analyzer.start();
        sine.start();

        Thread.sleep(2000);

        sine.stop();
        analyzer.stop();

        Thread.sleep(1000);

        System.out.println("Sine: " + sine.report());
        System.out.println("Analyzer: " + analyzer.report());
        
        int[] spectra = analyzer.getSpectra();
        
        
        assertEquals(1, spectra.length);
        assertEquals((double)50, (double)spectra[0], 5);
    }
    

}