/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.resource.dtmf;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.dtmf.DtmfDetectorListener;
import org.restcomm.media.spi.dtmf.DtmfEvent;
import org.restcomm.media.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class DtmfBufferingTest implements DtmfDetectorListener {
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    
    private DetectorImpl detector;
    private GeneratorImpl generator;
    
    private AudioComponent detectorComponent;
    private AudioComponent generatorComponent;
    private AudioMixer audioMixer;
    
    private OOBComponent oobDetectorComponent;
    private OOBComponent oobGeneratorComponent;
    private OOBMixer oobMixer;
    
    private String tone;
    
    @Before
    public void setUp() throws TooManyListenersException {
        clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new GeneratorImpl("dtmf", scheduler);
        generator.setToneDuration(100);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", -35, 40, 100, scheduler);
        
        audioMixer=new AudioMixer(scheduler);
        
        detectorComponent=new AudioComponent(1);
        detectorComponent.addOutput(detector.getAudioOutput());
        detectorComponent.updateMode(false,true);
        
        generatorComponent=new AudioComponent(2);
        generatorComponent.addInput(generator.getAudioInput());
        generatorComponent.updateMode(true,false);
                
        audioMixer.addComponent(detectorComponent);
        audioMixer.addComponent(generatorComponent);
        
        oobMixer=new OOBMixer(scheduler);
        
        oobDetectorComponent=new OOBComponent(1);
        oobDetectorComponent.addOutput(detector.getOOBOutput());
        oobDetectorComponent.updateMode(false,true);
        
        oobGeneratorComponent=new OOBComponent(2);
        oobGeneratorComponent.addInput(generator.getOOBInput());
        oobGeneratorComponent.updateMode(true,false);
        
        oobMixer.addComponent(oobDetectorComponent);
        oobMixer.addComponent(oobGeneratorComponent);
        
        tone="";
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
    public void testFlush() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);        

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(300);        
        
        assertEquals("1", tone);
        
        tone="";
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);        

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(300);        
        
        assertEquals("1", tone);
    }

    @Test
    public void testBuffering() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          
        
        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.wakeup();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
        
        tone="";
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          
        
        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.wakeup();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }

    @Test
    public void testDelivery() throws InterruptedException, TooManyListenersException {
        //assign listener
        detector.addListener(this);
        
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();    	

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("12", tone);
        
        //assign listener and flush digit
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
        
        tone="";
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();    	

        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.activate();
        detector.activate();
        oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        assertEquals("12", tone);
        
        //assign listener and flush digit
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }
    
    @Test
    public void testClear() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.clearBuffer();
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("", tone);
        
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();

        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.activate();
        detector.activate();
        oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.clearBuffer();
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("", tone);
    }        
    	
    public void process(DtmfEvent event) {
        tone += event.getTone();
    }
}
