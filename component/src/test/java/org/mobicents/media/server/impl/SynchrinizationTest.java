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

package org.mobicents.media.server.impl;

import org.mobicents.media.server.component.audio.*;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.io.Pipe;

/**
 *
 * @author kulikov
 */
public class SynchrinizationTest {

    private Clock clock;
    private Scheduler scheduler;

    private Sine sine;
    private SpectraAnalyzer analyzer;
    
    public SynchrinizationTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        analyzer = new SpectraAnalyzer("analyzer", scheduler);


        Pipe pipe = new PipeImpl();

        pipe.connect(sine);
        pipe.connect(analyzer);

        sine.setFrequency(50);
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    /**
     * Test of setAmplitude method, of class Sine.
     */
    @Test
    public void testSync() throws InterruptedException {
        analyzer.start();
        //generator not started but trying to synchornize
        sine.wakeup();

        Thread.sleep(5000);

        sine.stop();
        analyzer.stop();

        Thread.sleep(1000);

        int[] spectra = analyzer.getSpectra();

        assertEquals(0, spectra.length);
    }


}