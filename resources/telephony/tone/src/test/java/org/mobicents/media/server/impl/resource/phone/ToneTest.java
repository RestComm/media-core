/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.phone;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.spi.tone.ToneDetectorListener;
import org.mobicents.media.server.spi.tone.ToneEvent;

/**
 *
 * @author yulian oifa
 */
public class ToneTest implements ToneDetectorListener {

    private Clock clock;
    private Scheduler scheduler;

    private PhoneSignalDetector detector;
    private PhoneSignalGenerator generator;

    private InbandComponent detectorComponent;
    private InbandComponent generatorComponent;
    private AudioMixer audioMixer;

    private Boolean detected = false;

    @Before
    public void setUp() throws TooManyListenersException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        generator = new PhoneSignalGenerator("tone", scheduler);
        detector = new PhoneSignalDetector("tone", scheduler);

        detector.addListener(this);

        audioMixer = new AudioMixer(scheduler);

        detectorComponent = new InbandComponent(1);
        detectorComponent.addOutput(detector.getMediaOutput());
        detectorComponent.setReadable(false);
        detectorComponent.setWritable(true);

        generatorComponent = new InbandComponent(2);
        generatorComponent.addInput(generator.getMediaInput());
        generatorComponent.setReadable(true);
        generatorComponent.setWritable(false);

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
        generator.setFrequency(new int[] { 1700 });
        detector.setFrequency(new int[] { 1700 });
        generator.activate();
        detector.activate();
        audioMixer.start();

        Thread.sleep(5000);

        generator.deactivate();
        detector.deactivate();
        audioMixer.stop();

        assertEquals(true, detected);
    }

    @Override
    public void process(ToneEvent event) {
        detected = true;
    }
}
