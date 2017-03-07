/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.component.AbstractSource;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.PriorityQueueScheduler;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author yulian oifa
 */
public class AbstractSourceTest {

    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    static {
        formats.add(LINEAR);
    }
    
    private Clock clock;
    private PriorityQueueScheduler scheduler;
    
    private MyTestSource source;
    private MyTestSink sink;
    
    private Semaphore semaphore = new Semaphore(0);
    
    private long[] timestamp = new long[1000];
    private int count;
    
    @Before
    public void setUp() {
        clock = new WallClock();

        scheduler = new PriorityQueueScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        source = new MyTestSource(scheduler);
        sink = new MyTestSink();   
        
        source.connect(sink);
        count = 0;
    }
    
    @After
    public void tearDown() {
    	source.stop();
    	source.disconnect();
        scheduler.stop();    	
    }

    @Test
    public void testDuration() throws InterruptedException {
    	//originaly infinite stream: duration unknown
        assertEquals(-1, source.getDuration());
        
        //apply max duration and check value
        source.setDuration(3000000000L);
        assertEquals(3000000000L, source.getDuration());
        
        //start transmission
        source.start();
        
        //block for 5 seconds max
        long s = System.currentTimeMillis();
        semaphore.tryAcquire(4, TimeUnit.SECONDS);
        
        
        //check results
        long duration = System.currentTimeMillis() - s;
        assertFalse("Source still working", source.isStarted());
        assertEquals(3000, duration, 500);    	
    }
    
    /**
     * Test of setInitialDelay method, of class AbstractSource.
     */
    @Test
    public void testInitialDelay() throws InterruptedException {
        //apply max duration and check value
        source.setDuration(3000000000L);
        source.setInitialDelay(2000000000L);
        assertEquals(3000000000L, source.getDuration());
        
        //start transmission
        source.activate();
        
        //block for 5 seconds max
        long s = System.currentTimeMillis();
        semaphore.tryAcquire(6, TimeUnit.SECONDS);
        
        
        //check results
        long duration = System.currentTimeMillis() - s;
        assertFalse("Source still working", source.isStarted());
        assertEquals(5000, duration, 500);
    }


    /**
     * Test Media time
     */
    @Test
    public void testMediaTime() throws InterruptedException {
        //start transmission
        source.activate();
        
        Thread.sleep(1000);
        
        long time = source.getMediaTime();
        source.deactivate();
        
        Thread.sleep(100);
        
        System.out.println("Time=" + time);
        source.setMediaTime(time);
        source.activate();
        
        Thread.sleep(1000);
        
        assertTrue("Data expected", count > 0);
        for (int i = 0; i < count - 1; i++) {
            assertTrue("Time flows back", timestamp[i+1] - timestamp[i] >= 20000000L);
        }
    }
    
    /**
     * Test of setDsp method, of class AbstractSource.
     */

    public class MyTestSource extends AbstractSource {
        
		private static final long serialVersionUID = -2796811517778445960L;

		private long seq = 0;
        
        public MyTestSource(PriorityQueueScheduler scheduler) {
            super("", scheduler, PriorityQueueScheduler.OUTPUT_QUEUE);
        }

        @Override
        public Frame evolve(long timestamp) {
            Frame frame = Memory.allocate(320);
            frame.setOffset(0);
            frame.setLength(0);
            frame.setSequenceNumber(seq++);
            frame.setFormat(LINEAR);
            frame.setDuration(20000000L);
            
            return frame;
        }        
        
        @Override
        protected void completed() {
            super.completed();
            semaphore.release();
        }
    }
    
    private class MyTestSink extends AbstractSink {
        
		private static final long serialVersionUID = 1877357275120410315L;

		public MyTestSink() {
            super("");
        }

        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
            timestamp[count++] = frame.getTimestamp();
        }

        @Override
        public void deactivate() {
        }

        @Override
        public void activate() {
        }
        
    }
    
}
