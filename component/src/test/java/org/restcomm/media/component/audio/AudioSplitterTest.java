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

package org.restcomm.media.component.audio;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 *
 * @author yulian oifa
 */
public class AudioSplitterTest {

    private Clock clock;
    private PriorityQueueScheduler scheduler;

    private Sine sine;

    private SpectraAnalyzer analyzer1;
    private SpectraAnalyzer analyzer2;
    private SpectraAnalyzer analyzer3;

    private AudioComponent sineComponent;
    private AudioComponent analyzer1Component;
    private AudioComponent analyzer2Component;
    private AudioComponent analyzer3Component;
    
    private AudioSplitter splitter;

    public AudioSplitterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
    	clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        
        analyzer1 = new SpectraAnalyzer("analyzer-1",scheduler);
        analyzer2 = new SpectraAnalyzer("analyzer-1",scheduler);
        analyzer3 = new SpectraAnalyzer("analyzer-1",scheduler);

        sineComponent=new AudioComponent(1);
        sineComponent.addInput(sine.getAudioInput());
        sineComponent.updateMode(true,false);
        
        analyzer1Component=new AudioComponent(2);
        analyzer1Component.addOutput(analyzer1.getAudioOutput());
        analyzer1Component.updateMode(false,true);
        
        analyzer2Component=new AudioComponent(3);
        analyzer2Component.addOutput(analyzer2.getAudioOutput());
        analyzer2Component.updateMode(false,true);
        
        analyzer3Component=new AudioComponent(4);
        analyzer3Component.addOutput(analyzer3.getAudioOutput());
        analyzer3Component.updateMode(false,true);
        
        splitter = new AudioSplitter(scheduler);
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