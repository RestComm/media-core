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

package org.mobicents.media.server.component;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class SplitterTest {

    private Clock clock;
    private Scheduler scheduler;

    private Sine sine;

    private SpectraAnalyzer analyzer1;
    private SpectraAnalyzer analyzer2;
    private SpectraAnalyzer analyzer3;

    private MediaSource output1;
    private MediaSource output2;
    private MediaSource output3;
    
    private Splitter splitter;
//    private NetworkChannel network;

    public SplitterTest() {
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
        splitter = new Splitter(scheduler);
        
//        network = new NetworkChannel(scheduler, 9201);
//        scheduler.submit(network, 0);

        analyzer1 = new SpectraAnalyzer("analyzer-1");
        analyzer2 = new SpectraAnalyzer("analyzer-1");
        analyzer3 = new SpectraAnalyzer("analyzer-1");


        output1 = splitter.newOutput();
        output2 = splitter.newOutput();
        output3 = splitter.newOutput();

        output1.connect(analyzer1);
        output2.connect(analyzer2);
        output3.connect(analyzer3);
        sine.connect(splitter.getInput());
        
        sine.setFrequency(50);

        output1.start();
        output2.start();
        output3.start();
    }

    @After
    public void tearDown() throws InterruptedException {
        scheduler.stop();
//        network.close();
    }

    @Test
    public void testTransfer() throws InterruptedException {
        sine.start();

        output1.start();
        output2.start();
        output3.start();

        Thread.sleep(5000);

        sine.stop();

        output1.stop();
        output2.stop();
        output3.stop();

        System.out.println("Splitter: " + splitter.report());
        
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