/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.ivr;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.PipeImpl;
import org.mobicents.media.server.impl.resource.audio.AudioRecorderImpl;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author kulikov
 */
public class AudioSinkTest {

    private DefaultClock clock;
    private Scheduler scheduler;
    
    private AudioSink sink;
    private Sine sine;
    private SpectraAnalyzer analyzer;
    
    public AudioSinkTest() {
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
        sine.setAmplitude(Short.MAX_VALUE);
        sine.setFrequency(50);

        
        sink = new AudioSink(scheduler, "test");
        
        
        PipeImpl pipe = new PipeImpl();
        pipe.connect(sine);
        pipe.connect(sink);
    }
    
    @After
    public void tearDown() {
        scheduler.stop();
    }

    @Test
    public void testSilence() throws InterruptedException {
        analyzer = new SpectraAnalyzer("fft", scheduler);
        sink.add(analyzer);
        
        sink.start();
        analyzer.start();
        
        Thread.sleep(3000);
        
        sink.stop();
        analyzer.stop();
        
        int[] s = analyzer.getSpectra();
        
        assertEquals(0, s.length);
    }
    
    /**
     * Test of start method, of class AudioAnnouncement.
     */
    @Test
    public void testSignal() throws InterruptedException {
        analyzer = new SpectraAnalyzer("fft", scheduler);
        sink.add(analyzer);
        
        sink.start();
        analyzer.start();
        
        //MediaSource generator = (MediaSource) aap.getComponent(Sine.class);
        //generator.start();
        sine.start();
        
        Thread.sleep(3000);
        
//        generator.stop();
        sine.stop();
        sink.stop();
        analyzer.stop();
        
        int[] s = analyzer.getSpectra();
        
        assertEquals(1, s.length);
        assertEquals(50, s[0], 5);
    }

//    @Test
    public void testRecording() throws InterruptedException, IOException {
        AudioRecorderImpl  recorder = new AudioRecorderImpl(scheduler);
        recorder.setRecordFile("file:///home/kulikov/test-record.wav", false);
        sink.add(recorder);
        
        sink.start();
        recorder.start();
        
        sine.start();
        
        Thread.sleep(3000);
        
        sine.stop();
        sink.stop();
        
        recorder.stop();
    }
    
    @Test
    public void testGetComponent() {
        analyzer = new SpectraAnalyzer("fft", scheduler);
        sink.add(analyzer);
        
        assertTrue(sink.getComponent(SpectraAnalyzer.class) != null);
    }
}
