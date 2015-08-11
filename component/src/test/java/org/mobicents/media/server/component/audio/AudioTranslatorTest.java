/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.component.audio;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AudioTranslatorTest {

    private Clock clock;
    private Scheduler scheduler;

    private Sine sine1;
    private Sine sine2;
    private Sine sine3;

    private SpectraAnalyzer analyzer;
    private AudioTranslator translator;

    private InbandComponent sine1Component;
    private InbandComponent sine2Component;
    private InbandComponent sine3Component;
    private InbandComponent analyzerComponent;

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

        sine1Component = new InbandComponent(1);
        sine1Component.addInput(sine1.getMediaInput());
        sine1Component.setReadable(true);
        sine1Component.setWritable(false);

        sine2Component = new InbandComponent(2);
        sine2Component.addInput(sine2.getMediaInput());
        sine2Component.setReadable(true);
        sine2Component.setWritable(false);

        sine3Component = new InbandComponent(3);
        sine3Component.addInput(sine3.getMediaInput());
        sine3Component.setReadable(true);
        sine3Component.setWritable(false);

        analyzerComponent = new InbandComponent(4);
        analyzerComponent.addOutput(analyzer.getMediaOutput());
        analyzerComponent.setReadable(false);
        analyzerComponent.setWritable(true);

        translator = new AudioTranslator(scheduler);
        translator.addComponent(sine1Component);
        translator.addComponent(sine2Component);
        translator.addComponent(sine3Component);
        translator.addComponent(analyzerComponent);

        sine1.setAmplitude((short) (Short.MAX_VALUE / 4));
        sine2.setAmplitude((short) (Short.MAX_VALUE / 4));
        sine3.setAmplitude((short) (Short.MAX_VALUE / 4));

        // XXX Frequencies under 90 are not recognized
        sine1.setFrequency(100);
        sine2.setFrequency(150);
        sine3.setFrequency(250);
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    @Test
    public void testTranslate() throws InterruptedException {
        sine1.activate();
        sine2.activate();
        sine3.activate();
        analyzer.activate();

        translator.start();

        Thread.sleep(5000L);

        translator.stop();
        sine1.deactivate();
        sine2.deactivate();
        sine3.deactivate();
        analyzer.deactivate();

        System.out.println("Translator execution count: " + translator.getExecutionCount());

        int res[] = analyzer.getSpectra();
        assertEquals(3, res.length);
        assertEquals(100, res[0], 5);
        assertEquals(150, res[1], 5);
        assertEquals(250, res[2], 5);
    }

    @Test
    public void testManyTranslations() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.out.println("Translator test #" + i);
            testTranslate();
        }
    }

    @Test
    public void testRecycle() throws InterruptedException {
        testTranslate();

        translator.removeComponent(sine1Component);
        translator.addComponent(sine1Component);

        testTranslate();
    }

}
