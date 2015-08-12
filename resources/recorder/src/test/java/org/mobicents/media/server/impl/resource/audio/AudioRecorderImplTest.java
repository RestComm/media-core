/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.resource.audio;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class AudioRecorderImplTest {

    private Clock clock;
    private Scheduler scheduler;
    private DspFactoryImpl dspFactory;

    private Sine sine;
    private AudioRecorderImpl recorder;

    private InbandComponent sineComponent;
    private InbandComponent recorderComponent;

    private AudioMixer mixer;

    public AudioRecorderImplTest() {
        this.dspFactory = new DspFactoryImpl();
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");
    }

    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        sine.setFrequency(250);

        recorder = new AudioRecorderImpl(scheduler);

        sineComponent = new InbandComponent(1, dspFactory.newProcessor());
        recorderComponent = new InbandComponent(2, dspFactory.newProcessor());

        sineComponent.setReadable(true);
        sineComponent.setWritable(true);
        recorderComponent.setReadable(true);
        recorderComponent.setWritable(true);

        sineComponent.addInput(sine.getMediaInput());
        recorderComponent.addOutput(recorder.getMediaOutput());

        mixer = new AudioMixer(scheduler);
        mixer.addComponent(sineComponent);
        mixer.addComponent(recorderComponent);
    }

    /**
     * Test of start method, of class AudioRecorderImpl.
     */
    @Test
    public void testConvert() {
        System.out.println("======" + Integer.toHexString(8000));
    }

    /**
     * Test of stop method, of class AudioRecorderImpl. Check it manually
     */
    // @Test
    public void testRecording() throws InterruptedException, IOException {
        recorder.setRecordFile("file:///home/kulikov/record-test.wav", false);
        recorder.setPostSpeechTimer(5000000000L);

        sine.start();
        mixer.start();

        Thread.sleep(5000);
        sine.setAmplitude((short) 0);

        Thread.sleep(7000);
        sine.stop();
        mixer.stop();
    }

}
