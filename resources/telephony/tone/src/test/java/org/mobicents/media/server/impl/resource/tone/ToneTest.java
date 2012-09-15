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

import org.mobicents.media.server.component.audio.CompoundComponent;
import org.mobicents.media.server.component.audio.CompoundMixer;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;
import org.mobicents.media.server.spi.tone.ToneEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class ToneTest implements ToneDetectorListener {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private PhoneSignalDetector detector;
    private PhoneSignalGenerator generator;
    
    private CompoundComponent detectorComponent;
    private CompoundComponent generatorComponent;
    private CompoundMixer compoundMixer;
    
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
    	clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new PhoneSignalGenerator("tone", scheduler);        
        detector = new PhoneSignalDetector("tone", scheduler);
        
        detector.addListener(this);
        
        compoundMixer=new CompoundMixer(scheduler);
        
        detectorComponent=new CompoundComponent(1);
        detectorComponent.addOutput(detector.getCompoundOutput());
        detectorComponent.updateMode(false,true);
        
        generatorComponent=new CompoundComponent(2);
        generatorComponent.addInput(generator.getCompoundInput());
        generatorComponent.updateMode(true,false);
        
        compoundMixer.addComponent(detectorComponent);
        compoundMixer.addComponent(generatorComponent);    	
    }
    
    @After
    public void tearDown() {
    	generator.deactivate();
    	detector.deactivate();
    	compoundMixer.stop();
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
    	compoundMixer.start();
        
        Thread.sleep(5000);
        
        generator.deactivate();
        detector.deactivate();
    	compoundMixer.stop();
    	
        assertEquals(true, detected);    	
    }    
    
    public void process(ToneEvent event) {
    	detected = true;
    }
}
