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

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class AudioForwardingSplitterTest {

    private Clock clock;
    private Scheduler scheduler;
    private DspFactoryImpl dspFactory;

    private Sine sine;

    private SpectraAnalyzer analyzer1;
    private SpectraAnalyzer analyzer2;
    private SpectraAnalyzer analyzer3;

    private InbandComponent sineComponent;
    private InbandComponent analyzer1Component;
    private InbandComponent analyzer2Component;
    private InbandComponent analyzer3Component;

    private AudioForwardingSplitter splitter;

    public AudioForwardingSplitterTest() {
        this.dspFactory = new DspFactoryImpl();
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
    }

    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);

        analyzer1 = new SpectraAnalyzer("analyzer-1", scheduler);
        analyzer2 = new SpectraAnalyzer("analyzer-2", scheduler);
        analyzer3 = new SpectraAnalyzer("analyzer-3", scheduler);

        sineComponent = new InbandComponent(1, dspFactory.newProcessor());
        sineComponent.addInput(sine.getMediaInput());
        sineComponent.setReadable(true);
        sineComponent.setWritable(false);

        analyzer1Component = new InbandComponent(2, dspFactory.newProcessor());
        analyzer1Component.addOutput(analyzer1.getMediaOutput());
        analyzer1Component.setReadable(false);
        analyzer1Component.setWritable(true);

        analyzer2Component = new InbandComponent(3, dspFactory.newProcessor());
        analyzer2Component.addOutput(analyzer2.getMediaOutput());
        analyzer2Component.setReadable(false);
        analyzer2Component.setWritable(true);

        analyzer3Component = new InbandComponent(4, dspFactory.newProcessor());
        analyzer3Component.addOutput(analyzer3.getMediaOutput());
        analyzer3Component.setReadable(false);
        analyzer3Component.setWritable(true);

        splitter = new AudioForwardingSplitter(scheduler);
        splitter.addInsideComponent(sineComponent);
        splitter.addOutsideComponent(analyzer1Component);
        splitter.addOutsideComponent(analyzer2Component);
        splitter.addOutsideComponent(analyzer3Component);

        sine.setFrequency(50);
    }

    @After
    public void tearDown() throws InterruptedException {
        scheduler.stop();
    }

    @Test
    public void testTransfer() throws InterruptedException {
        sine.activate();
        splitter.start();
        analyzer1.activate();
        analyzer2.activate();
        analyzer3.activate();

        Thread.sleep(5000);

        sine.deactivate();
        splitter.stop();
        analyzer1.deactivate();
        analyzer2.deactivate();
        analyzer3.deactivate();

        int res[] = analyzer1.getSpectra();
        assertEquals(1, res.length);
        assertEquals(50, res[0], 5);

        res = analyzer2.getSpectra();
        assertEquals(1, res.length);
        assertEquals(50, res[0], 5);

        res = analyzer3.getSpectra();
        assertEquals(1, res.length);
        assertEquals(50, res[0], 5);
    }
}