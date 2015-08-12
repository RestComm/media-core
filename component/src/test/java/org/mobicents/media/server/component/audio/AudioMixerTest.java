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
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class AudioMixerTest {

    private Clock clock;
    private Scheduler scheduler;
    private DspFactoryImpl dspFactory;

    private Sine sine1;
    private Sine sine2;
    private Sine sine3;

    private SpectraAnalyzer analyzer;
    private AudioMixer mixer;

    private InbandComponent sine1Component;
    private InbandComponent sine2Component;
    private InbandComponent sine3Component;
    private InbandComponent analyzerComponent;

    public AudioMixerTest() {
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

        sine1 = new Sine(scheduler);
        sine2 = new Sine(scheduler);
        sine3 = new Sine(scheduler);
        analyzer = new SpectraAnalyzer("analyzer", scheduler);

        sine1Component = new InbandComponent(1, dspFactory.newProcessor());
        sine1Component.addInput(sine1.getMediaInput());
        sine1Component.setReadable(true);
        sine1Component.setWritable(false);

        sine2Component = new InbandComponent(2, dspFactory.newProcessor());
        sine2Component.addInput(sine2.getMediaInput());
        sine1Component.setReadable(true);
        sine1Component.setWritable(false);

        sine3Component = new InbandComponent(3, dspFactory.newProcessor());
        sine3Component.addInput(sine3.getMediaInput());
        sine1Component.setReadable(true);
        sine1Component.setWritable(false);

        analyzerComponent = new InbandComponent(4, dspFactory.newProcessor());
        analyzerComponent.addOutput(analyzer.getMediaOutput());
        analyzerComponent.setReadable(false);
        analyzerComponent.setWritable(true);

        mixer = new AudioMixer(scheduler);
        mixer.addComponent(sine1Component);
        mixer.addComponent(sine2Component);
        mixer.addComponent(sine3Component);
        mixer.addComponent(analyzerComponent);

        sine1.setAmplitude((short) (Short.MAX_VALUE / 4));
        sine2.setAmplitude((short) (Short.MAX_VALUE / 4));
        sine3.setAmplitude((short) (Short.MAX_VALUE / 4));

        sine1.setFrequency(80);
        sine2.setFrequency(150);
        sine3.setFrequency(250);
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    /**
     * Test of setScheduler method, of class AudioMixer.
     */
    @Test
    public void testDefaultPacketSize() {
        assertEquals(320, mixer.getPacketSize());
    }

    @Test
    public void testMixing() throws InterruptedException {
        sine1.activate();
        sine2.activate();
        sine3.activate();
        analyzer.activate();

        mixer.start();

        Thread.sleep(5000);

        mixer.stop();
        sine1.deactivate();
        sine2.deactivate();
        sine3.deactivate();
        analyzer.deactivate();

        System.out.println("mix execution count: " + mixer.getMixCount());
        // System.out.println("IO count: " + network.getCount());
        // System.out.println("Mixer input: " + ((AbstractSink)input1).getCount());

        int res[] = analyzer.getSpectra();
        assertEquals(3, res.length);
        assertEquals(80, res[0], 5);
        assertEquals(150, res[1], 5);
        assertEquals(250, res[2], 5);

    }

    @Test
    public void testGain() throws InterruptedException {
        sine1.activate();
        sine2.activate();
        sine3.activate();

        mixer.setGain(-10);
        mixer.start();
        analyzer.activate();

        Thread.sleep(5000);

        mixer.stop();
        sine1.deactivate();
        sine2.deactivate();
        sine3.deactivate();
        analyzer.deactivate();

        int res[] = analyzer.getSpectra();
        assertEquals(0, res.length);
    }

    @Test
    public void testMixingFailure() throws InterruptedException {
        int N = 5;// 100;
        for (int i = 0; i < N; i++) {
            System.out.println("Test # " + i);
            testMixing();
        }
    }

    @Test
    public void testRecycle() throws InterruptedException {
        testMixing();

        mixer.removeComponent(sine1Component);
        mixer.addComponent(sine1Component);

        testMixing();
    }
}