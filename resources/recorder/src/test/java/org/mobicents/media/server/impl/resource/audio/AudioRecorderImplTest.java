/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.audio;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.component.audio.CompoundInput;
import org.mobicents.media.server.component.audio.CompoundOutput;
import org.mobicents.media.server.component.audio.CompoundComponent;
import org.mobicents.media.server.component.audio.CompoundMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class AudioRecorderImplTest {
    
    private Clock clock;
    private Scheduler scheduler;

    private Sine sine;
    private AudioRecorderImpl recorder;
    
    private CompoundComponent sineComponent;
    private CompoundComponent recorderComponent;
    
    private CompoundMixer mixer;
    
    public AudioRecorderImplTest() {
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
        sine.setFrequency(250);
        
        recorder = new AudioRecorderImpl(scheduler);
        
        sineComponent=new CompoundComponent(1);
        recorderComponent=new CompoundComponent(2);
        
        sineComponent.updateMode(true,true);
        recorderComponent.updateMode(true,true);
        
        sineComponent.addInput(sine.getCompoundInput());
        recorderComponent.addOutput(recorder.getCompoundOutput());
        
        mixer=new CompoundMixer(scheduler);
        mixer.addComponent(sineComponent);
        mixer.addComponent(recorderComponent);               
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of start method, of class AudioRecorderImpl.
     */
    @Test
    public void testConvert() {
        System.out.println("======" + Integer.toHexString(8000));
    }

    /**
     * Test of stop method, of class AudioRecorderImpl.
     * Check it manually
     */
//    @Test
    public void testRecording() throws InterruptedException, IOException {
        recorder.setRecordFile("file:///home/kulikov/record-test.wav", false);
        recorder.setPostSpeechTimer(5000000000L);
        
        sine.start();  
        mixer.start();
        
        Thread.sleep(5000);        
        sine.setAmplitude((short)0);
        
        Thread.sleep(7000);
        sine.stop();
        mixer.stop();
    }

}
