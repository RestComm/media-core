/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.phone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.WallClock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;
import org.mobicents.media.server.spi.tone.ToneEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class ToneTest implements ToneDetectorListener {
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    
    private PhoneSignalDetector detector;
    private PhoneSignalGenerator generator;
    
    private AudioComponent detectorComponent;
    private AudioComponent generatorComponent;
    private AudioMixer audioMixer;
    
    private Boolean detected=false;
    
    public ToneTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws TooManyListenersException {
    	clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new PhoneSignalGenerator("tone", scheduler);        
        detector = new PhoneSignalDetector("tone", scheduler);
        
        detector.addListener(this);
        
        audioMixer=new AudioMixer(scheduler);
        
        detectorComponent=new AudioComponent(1);
        detectorComponent.addOutput(detector.getAudioOutput());
        detectorComponent.updateMode(false,true);
        
        generatorComponent=new AudioComponent(2);
        generatorComponent.addInput(generator.getAudioInput());
        generatorComponent.updateMode(true,false);
        
        audioMixer.addComponent(detectorComponent);
        audioMixer.addComponent(generatorComponent);    	
    }
    
    @After
    public void tearDown() {
    	generator.deactivate();
    	detector.deactivate();
    	audioMixer.stop();
        scheduler.stop();
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */
    @Test
    public void testTone() throws InterruptedException {
    	generator.setFrequency(new int[] {1700});
    	detector.setFrequency(new int[] {1700});
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(5000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
    	
        assertEquals(true, detected);    	
    }    
    
    public void process(ToneEvent event) {
    	detected = true;
    }
}
