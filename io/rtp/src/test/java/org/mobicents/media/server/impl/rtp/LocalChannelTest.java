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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 *
 * @author oifa yulian
 */
public class LocalChannelTest {

    // clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    private ChannelsManager channelsManager;
    private UdpManager udpManager;
    private DspFactory dspFactory;

    private SpectraAnalyzer analyzer1, analyzer2;
    private Sine source1, source2;

    private LocalChannel channel1, channel2;

    private int fcount;

    private AudioMixer audioMixer1, audioMixer2;
    private InbandComponent component1, component2;

    public LocalChannelTest() {
    }

    @Before
    public void setUp() throws Exception {
        // use default clock
        clock = new DefaultClock();

        // create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.start();

        this.dspFactory = new DspFactoryImpl();
        channelsManager = new ChannelsManager(udpManager, dspFactory);
        channelsManager.setScheduler(scheduler);

        source1 = new Sine(scheduler);
        source1.setFrequency(50);

        source2 = new Sine(scheduler);
        source2.setFrequency(100);

        analyzer1 = new SpectraAnalyzer("analyzer", scheduler);
        analyzer2 = new SpectraAnalyzer("analyzer", scheduler);

        channel1 = channelsManager.getLocalChannel();
        channel2 = channelsManager.getLocalChannel();
        channel1.join(channel2);

        audioMixer1 = new AudioMixer(scheduler);
        audioMixer2 = new AudioMixer(scheduler);

        component1 = new InbandComponent(1);
        component1.addInput(source1.getAudioInput());
        component1.addOutput(analyzer1.getAudioOutput());
        component1.setReadable(true);
        component1.setWritable(true);

        audioMixer1.addComponent(component1);
        audioMixer1.addComponent(channel1.getMediaComponent().getInbandComponent());

        component2 = new InbandComponent(2);
        component2.addInput(source2.getAudioInput());
        component2.addOutput(analyzer2.getAudioOutput());
        component2.setReadable(true);
        component2.setWritable(true);

        audioMixer2.addComponent(component2);
        audioMixer2.addComponent(channel2.getMediaComponent().getInbandComponent());
    }

    @After
    public void tearDown() {
        source1.deactivate();
        source2.deactivate();
        analyzer1.deactivate();
        analyzer2.deactivate();

        channel1.unjoin();
        channel2.unjoin();

        audioMixer1.stop();
        audioMixer2.stop();

        udpManager.stop();
        scheduler.stop();
    }

    @Test
    public void testTransmission() throws Exception {
        channel1.updateMode(ConnectionMode.SEND_RECV);
        channel2.updateMode(ConnectionMode.SEND_RECV);

        source1.activate();
        source2.activate();
        analyzer1.activate();
        analyzer2.activate();
        audioMixer1.start();
        audioMixer2.start();

        Thread.sleep(5000);

        analyzer1.deactivate();
        analyzer2.deactivate();
        source1.deactivate();
        source2.deactivate();
        audioMixer1.stop();
        audioMixer2.stop();
        channel1.updateMode(ConnectionMode.INACTIVE);

        int s1[] = analyzer1.getSpectra();
        int s2[] = analyzer2.getSpectra();

        if (s1.length != 1 || s2.length != 1) {
            fcount++;
            System.out.println("Failure ,s1:" + s1.length + ",s2:" + s2.length + ". Total failures: " + fcount);
        } else
            System.out.println("Passed");

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);

        assertEquals(100, s1[0], 5);
        assertEquals(50, s2[0], 5);
    }
}