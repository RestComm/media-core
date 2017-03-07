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

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.component.audio.SpectraAnalyzer;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 *
 * @author yulian oifa
 */
public class SineTest {

    private Clock clock;
    private PriorityQueueScheduler scheduler;

    private Sine sine;
    private SpectraAnalyzer analyzer;
    
    private AudioComponent sineComponent;
    private AudioComponent analyzerComponent;
    
    private AudioMixer audioMixer;
    
    public SineTest() {
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
        analyzer = new SpectraAnalyzer("analyzer",scheduler);

        sineComponent=new AudioComponent(1);
        sineComponent.addInput(sine.getAudioInput());
        sineComponent.updateMode(true,false);
        
        analyzerComponent=new AudioComponent(2);
        analyzerComponent.addOutput(analyzer.getAudioOutput());
        analyzerComponent.updateMode(false,true);
        
        audioMixer=new AudioMixer(scheduler);
        audioMixer.addComponent(sineComponent);
        audioMixer.addComponent(analyzerComponent); 
        
        sine.setFrequency(50);
    }

    @After
    public void tearDown() {
    	sine.stop();
    	audioMixer.stop();
    	audioMixer.release(sineComponent);
    	audioMixer.release(analyzerComponent);
    	
        scheduler.stop();
    }

    /**
     * Test of setAmplitude method, of class Sine.
     */
    @Test
    public void testSignal() throws Exception {
        sine.activate();
        analyzer.activate();
        audioMixer.start();
        
        Thread.sleep(2000);

        sine.deactivate();
        analyzer.deactivate();
        audioMixer.stop();
        
        Thread.sleep(1000);

        int[] spectra = analyzer.getSpectra();

        assertEquals(1, spectra.length);
        assertEquals((double)50, (double)spectra[0], 5);
        
        sine.setAmplitude((short)0);        
        sine.activate();
        analyzer.activate();
        audioMixer.start();
        
        Thread.sleep(1000);
        sine.deactivate();
        analyzer.deactivate();
        audioMixer.stop();
        
        spectra = analyzer.getSpectra();
        assertEquals(0, spectra.length);
    }

//    @Test
    public void testSignalFailure() throws Exception {
        int N = 5; //500;
        for (int i = 0; i < N; i++) {
            testSignal();
            System.out.println("Test pass # " + i);
        }
    }

}