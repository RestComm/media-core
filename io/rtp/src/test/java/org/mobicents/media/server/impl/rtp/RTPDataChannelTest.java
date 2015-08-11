/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.Dsp;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;

/**
 *
 * @author oifa yulian
 */
@Deprecated
public class RTPDataChannelTest {

    // clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    private ChannelsManager channelsManager;
    private UdpManager udpManager;

    private SpectraAnalyzer analyzer1, analyzer2;
    private Sine source1, source2;

    private RTPDataChannel channel1, channel2;

    private int fcount;

    private DspFactoryImpl dspFactory = new DspFactoryImpl();

    private Dsp dsp11, dsp12;
    private Dsp dsp21, dsp22;

    private AudioMixer audioMixer1, audioMixer2;
    private InbandComponent component1, component2;

    public RTPDataChannelTest() {
    }

    @Before
    public void setUp() throws Exception {
        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        Formats fmts = new Formats();
        fmts.add(pcma);

        Formats dstFormats = new Formats();
        dstFormats.add(FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1));

        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

        dsp11 = dspFactory.newProcessor();
        dsp12 = dspFactory.newProcessor();

        dsp21 = dspFactory.newProcessor();
        dsp22 = dspFactory.newProcessor();

        // use default clock
        clock = new DefaultClock();

        // create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager, dspFactory);
        channelsManager.setScheduler(scheduler);

        source1 = new Sine(scheduler);
        source1.setFrequency(100);

        source2 = new Sine(scheduler);
        source2.setFrequency(50);

        analyzer1 = new SpectraAnalyzer("analyzer", scheduler);
        analyzer2 = new SpectraAnalyzer("analyzer", scheduler);

        channel1 = channelsManager.getChannel();
        channel1.updateMode(ConnectionMode.SEND_RECV);
        channel1.setOutputDsp(dsp11);
        channel1.setOutputFormats(fmts);
        channel1.setInputDsp(dsp12);

        channel2 = channelsManager.getChannel();
        channel2.updateMode(ConnectionMode.SEND_RECV);
        channel2.setOutputDsp(dsp21);
        channel2.setOutputFormats(fmts);
        channel2.setInputDsp(dsp22);

        channel1.bind(false);
        channel2.bind(false);

        channel1.setPeer(new InetSocketAddress("127.0.0.1", channel2.getLocalPort()));
        channel2.setPeer(new InetSocketAddress("127.0.0.1", channel1.getLocalPort()));

        channel1.setFormatMap(AVProfile.audio);
        channel2.setFormatMap(AVProfile.audio);

        audioMixer1 = new AudioMixer(scheduler);
        audioMixer2 = new AudioMixer(scheduler);

        component1 = new InbandComponent(1);
        component1.addInput(source1.getMediaInput());
        component1.addOutput(analyzer1.getMediaOutput());
        component1.setReadable(true);
        component1.setWritable(true);

        audioMixer1.addComponent(component1);
        audioMixer1.addComponent(channel1.getAudioComponent());

        component2 = new InbandComponent(2);
        component2.addInput(source2.getMediaInput());
        component2.addOutput(analyzer2.getMediaOutput());
        component2.setReadable(true);
        component2.setWritable(true);

        audioMixer2.addComponent(component2);
        audioMixer2.addComponent(channel2.getAudioComponent());
    }

    @After
    public void tearDown() {
        source1.deactivate();
        channel1.close();

        source2.deactivate();
        channel2.close();

        audioMixer1.stop();
        audioMixer2.stop();

        udpManager.stop();
        scheduler.stop();
    }

    @Test
    public void testTransmission() throws Exception {
        source1.activate();
        analyzer1.activate();
        audioMixer1.start();

        source2.start();
        analyzer2.activate();
        audioMixer2.start();

        Thread.sleep(5000);

        analyzer1.deactivate();
        analyzer2.deactivate();
        source1.deactivate();
        source2.deactivate();
        audioMixer1.stop();
        audioMixer2.stop();

        // Thread.sleep(5000);

        int s1[] = analyzer1.getSpectra();
        int s2[] = analyzer2.getSpectra();

        // print(s1);
        // print(s2);

        System.out.println("rx-channel1: " + channel1.getPacketsReceived());
        System.out.println("tx-channel1: " + channel1.getPacketsTransmitted());

        System.out.println("rx-channel2: " + channel2.getPacketsReceived());
        System.out.println("tx-channel2: " + channel2.getPacketsTransmitted());

        if (s1.length != 1 || s2.length != 1) {
            System.out.println("Failure ,s1:" + s1.length + ",s2:" + s2.length);
            fcount++;
        } else
            System.out.println("Passed");

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);
        assertEquals(50, s1[0], 5);
        assertEquals(100, s2[0], 5);
    }

    @Test
    public void testHalfDuplex() throws Exception {
        channel1.updateMode(ConnectionMode.RECV_ONLY);
        channel2.updateMode(ConnectionMode.SEND_ONLY);
        source1.activate();
        source2.activate();
        analyzer1.activate();
        audioMixer1.start();
        audioMixer2.start();

        Thread.sleep(5000);

        source1.deactivate();
        source2.deactivate();
        analyzer1.deactivate();
        audioMixer1.stop();
        audioMixer2.stop();

        int s1[] = analyzer1.getSpectra();
        int s2[] = analyzer2.getSpectra();

        System.out.println("rx-channel1: " + channel1.getPacketsReceived());
        System.out.println("tx-channel1: " + channel1.getPacketsTransmitted());

        System.out.println("rx-channel2: " + channel2.getPacketsReceived());
        System.out.println("tx-channel2: " + channel2.getPacketsTransmitted());

        if (s2.length != 0 || s1.length != 1) {
            fcount++;
        } else
            System.out.println("Passed");

        assertEquals(0, fcount);
        assertEquals(50, s1[0], 5);
    }

    @Test
    public void testFailureRate() throws Exception {
        for (int i = 0; i < 1; i++) {
            System.out.println("Test# " + i);
            this.testTransmission();
        }
        assertEquals(0, fcount);
    }

    // private void print(int[] s) {
    // for (int i = 0; i < s.length; i++) {
    // System.out.print(s[i] + " ");
    // }
    // System.out.println();
    // }
}