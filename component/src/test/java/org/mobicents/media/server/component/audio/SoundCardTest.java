/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.component.audio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.InbandComponent;
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

    private InbandComponent sineComponent;
    private InbandComponent soundCardComponent;

    private AudioMixer audioMixer;

    @Before
    public void setUp() {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        sine.setFrequency(200);

        soundCard = new SoundCard(scheduler);

        sineComponent = new InbandComponent(1);
        sineComponent.addInput(sine.getMediaInput());
        sineComponent.setReadable(true);
        sineComponent.setWritable(false);

        soundCardComponent = new InbandComponent(2);
        soundCardComponent.addOutput(soundCard.getMediaOutput());
        soundCardComponent.setReadable(false);
        soundCardComponent.setWritable(true);

        audioMixer = new AudioMixer(scheduler);
        audioMixer.addComponent(soundCardComponent);
        audioMixer.addComponent(sineComponent);
    }

    @After
    public void tearDown() {
        sine.stop();
        audioMixer.stop();
        audioMixer.removeComponent(sineComponent);
        audioMixer.removeComponent(soundCardComponent);

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
