/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.dtmf;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author kulikov
 */
public class DtmfBufferingTest implements DtmfDetectorListener {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private DetectorImpl detector;
    private GeneratorImpl generator;
    
    private PipeImpl pipe = new PipeImpl();
    private String tone;
    
    public DtmfBufferingTest() {
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
        
        generator = new GeneratorImpl("dtmf", scheduler);
        generator.setToneDuration(100);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        
        generator.connect(pipe);
        detector.connect(pipe);
        
        detector.start();
        tone="";
    }
    
    @After
    public void tearDown() {
        pipe.stop();
        scheduler.stop();
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */
    @Test
    public void testFlush() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.start();
        
        Thread.sleep(200);        
        generator.stop();
        
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
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();
        
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
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();
        
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
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.start();
        
        Thread.sleep(200);          
        generator.stop();
        
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
