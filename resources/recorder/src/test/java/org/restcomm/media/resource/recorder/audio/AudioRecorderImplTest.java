/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.resource.recorder.audio;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioInput;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.audio.AudioOutput;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.resource.recorder.audio.AudioRecorderImpl;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 *
 * @author yulian oifa
 */
public class AudioRecorderImplTest {
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;

    private Sine sine;
    private AudioRecorderImpl recorder;
    
    private AudioComponent sineComponent;
    private AudioComponent recorderComponent;
    
    private AudioMixer mixer;
    
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
        clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler); 
        sine.setFrequency(250);
        
        recorder = new AudioRecorderImpl(scheduler);
        
        sineComponent=new AudioComponent(1);
        recorderComponent=new AudioComponent(2);
        
        sineComponent.updateMode(true,true);
        recorderComponent.updateMode(true,true);
        
        sineComponent.addInput(sine.getAudioInput());
        recorderComponent.addOutput(recorder.getAudioOutput());
        
        mixer=new AudioMixer(scheduler);
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
