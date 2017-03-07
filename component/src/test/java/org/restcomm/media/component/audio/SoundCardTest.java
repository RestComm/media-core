/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.component.audio;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.audio.Sine;
import org.restcomm.media.component.audio.SoundCard;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 *
 * @author oifa yulian
 */
public class SoundCardTest {
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    
    private Sine sine;
    private SoundCard soundCard;
    
    private AudioComponent sineComponent;
    private AudioComponent soundCardComponent;
    
    private AudioMixer audioMixer;
    
    public SoundCardTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        sine.setFrequency(200);
        
        soundCard = new SoundCard(scheduler);
        
        sineComponent=new AudioComponent(1);
        sineComponent.addInput(sine.getAudioInput());
        sineComponent.updateMode(true,false);
        
        soundCardComponent=new AudioComponent(2);
        soundCardComponent.addOutput(soundCard.getAudioOutput());
        soundCardComponent.updateMode(false,true);
        
        audioMixer=new AudioMixer(scheduler);
        audioMixer.addComponent(soundCardComponent);
        audioMixer.addComponent(sineComponent);               
    }
    
    @After
    public void tearDown() {
    	sine.stop();
    	audioMixer.stop();
    	audioMixer.release(sineComponent);
    	audioMixer.release(soundCardComponent);
    	
        scheduler.stop();
    }

    /**
     * Test of start method, of class SoundCard.
     */
    @Test
    public void testSignal() throws InterruptedException {
    	sine.start();        
    	audioMixer.start();
        Thread.sleep(3000);        
        sine.stop();
        audioMixer.stop();
    }
}
