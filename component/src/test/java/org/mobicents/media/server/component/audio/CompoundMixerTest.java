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

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.MediaSink;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 *
 * @author yulian oifa
 */
public class CompoundMixerTest {

    private Clock clock;
    private Scheduler scheduler;

    private Sine sine1;
    private Sine sine2;
    private Sine sine3;

    private SpectraAnalyzer analyzer;
    private CompoundMixer mixer;

    private CompoundComponent sine1Component;
    private CompoundComponent sine2Component;
    private CompoundComponent sine3Component;
    private CompoundComponent analyzerComponent;
    
    public CompoundMixerTest() {
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

        sine1 = new Sine(scheduler);
        sine2 = new Sine(scheduler);
        sine3 = new Sine(scheduler);
        analyzer = new SpectraAnalyzer("analyzer",scheduler);
        
        sine1Component=new CompoundComponent(1);
        sine1Component.addInput(sine1.getCompoundInput());
        sine1Component.updateMode(true,false);
        
        sine2Component=new CompoundComponent(2);
        sine2Component.addInput(sine2.getCompoundInput());
        sine2Component.updateMode(true,false);
        
        sine3Component=new CompoundComponent(3);
        sine3Component.addInput(sine3.getCompoundInput());
        sine3Component.updateMode(true,false);
        
        analyzerComponent=new CompoundComponent(4);
        analyzerComponent.addOutput(analyzer.getCompoundOutput());
        analyzerComponent.updateMode(false,true);
        
        mixer = new CompoundMixer(scheduler);
        mixer.addComponent(sine1Component);
        mixer.addComponent(sine2Component);
        mixer.addComponent(sine3Component);
        mixer.addComponent(analyzerComponent); 
        
        sine1.setAmplitude((short)(Short.MAX_VALUE / 4));
        sine2.setAmplitude((short)(Short.MAX_VALUE / 4));
        sine3.setAmplitude((short)(Short.MAX_VALUE / 4));

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
//    @Test
    public void testDeafultPacketSize() {
        assertEquals(320, mixer.getPacketSize());
    }

//    @Test
    public void testMixing() throws InterruptedException {
        sine1.activate();
        sine2.activate();
        sine3.activate();
        analyzer.activate();

        mixer.start();
        
//        mixer.start();
        Thread.sleep(5000);

        mixer.stop();
        sine1.deactivate();
        sine2.deactivate();
        sine3.deactivate();
        analyzer.deactivate();
        
        System.out.println("mix execution count: " + mixer.mixCount);
//        System.out.println("IO count: " + network.getCount());
//        System.out.println("Mixer input: " + ((AbstractSink)input1).getCount());
        
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
        int N = 5;//100;
        for (int i = 0; i < N; i++) {
            System.out.println("Test # " + i);
            testMixing();
        }
    }
    
    @Test
    public void testRecycle() throws InterruptedException {
    	testMixing();
        
    	mixer.release(sine1Component);        
    	mixer.addComponent(sine1Component);
                
        testMixing();    	
    }
}