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
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.dtmf.DtmfDetectorListener;
import org.mobicents.media.server.spi.dtmf.DtmfEvent;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class DtmfTest implements DtmfDetectorListener {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private DetectorImpl detector;
    private GeneratorImpl generator;
    
    private String tone;
    
    public DtmfTest() {
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
        generator.setToneDuration(500);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        
        generator.connect(detector);        
        detector.addListener(this);        
    }
    
    @After
    public void tearDown() {
    	generator.stop();
        scheduler.stop();
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */
    @Test
    public void testDigit1() throws InterruptedException {
        generator.setDigit("1");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("1", tone);
    }

    @Test
    public void testDigit2() throws InterruptedException {
        generator.setDigit("2");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("2", tone);
    }
    
    @Test
    public void testDigit3() throws InterruptedException {
        generator.setDigit("3");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("3", tone);
    }

    @Test
    public void testDigit4() throws InterruptedException {
        generator.setDigit("4");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("4", tone);
    }
    
    @Test
    public void testDigit5() throws InterruptedException {
        generator.setDigit("5");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("5", tone);
    }
    
    @Test
    public void testDigit6() throws InterruptedException {
        generator.setDigit("6");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("6", tone);        
    }
    
    @Test
    public void testDigit7() throws InterruptedException {
        generator.setDigit("7");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("7", tone);
    }
    
    @Test
    public void testDigit8() throws InterruptedException {
        generator.setDigit("8");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("8", tone);
    }
    
    @Test
    public void testDigit9() throws InterruptedException {
        generator.setDigit("9");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("9", tone);
    }
    
    @Test
    public void testDigit0() throws InterruptedException {
        generator.setDigit("0");
        generator.start();
        
        Thread.sleep(1000);
        assertEquals("0", tone);
    }
    
    public void process(DtmfEvent event) {
        tone = event.getTone();
    }
}
