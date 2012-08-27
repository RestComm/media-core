/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.component.audio;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author oifa yulian
 */
public class SoundCardTest {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private Sine sine;
    private SoundCard soundCard;
    
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
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        sine.setFrequency(200);
        
        soundCard = new SoundCard();
        
        sine.connect(soundCard);        
    }
    
    @After
    public void tearDown() {
    	sine.stop();
    	sine.disconnect();
        scheduler.stop();
    }

    /**
     * Test of start method, of class SoundCard.
     */
    @Test
    public void testSignal() throws InterruptedException {
    	sine.start();        
        Thread.sleep(3000);        
        sine.stop();
    }

}
