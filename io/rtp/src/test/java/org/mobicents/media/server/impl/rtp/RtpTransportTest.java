/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.component.InbandComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.audio.Sine;
import org.mobicents.media.server.component.audio.SpectraAnalyzer;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * 
 * @author oifa yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTransportTest {

    private static final int CHANNEL1_ID = 99998;
    private static final int CHANNEL2_ID = 99999;

    private Scheduler scheduler;
    private UdpManager udpManager;
    private final DspFactoryImpl dspFactory;
    private ChannelsManager channelsManager;

    private SpectraAnalyzer analyzer1, analyzer2;
    private Sine source1, source2;

    private RtpTransport channel1, channel2;
    private RtpClock rtpClock1, rtpClock2;
    private RtpClock oobClock1, oobClock2;
    private final RTPFormats rtpFormats;
    private RtpStatistics statistics1, statistics2;

    private final AudioFormat pcma;

    private AudioMixer audioMixer1, audioMixer2;
    private RtpComponent rtpComponent1, rtpComponent2;

    public RtpTransportTest() throws IOException {
        // Scheduler
        this.scheduler = new Scheduler();
        scheduler.setClock(new DefaultClock());

        // UDP manager
        this.udpManager = new UdpManager(scheduler);

        // Digital Signaling Processor
        this.dspFactory = new DspFactoryImpl();
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

        // Channels manager
        this.channelsManager = new ChannelsManager(udpManager, dspFactory);
        this.channelsManager.setScheduler(scheduler);

        // Audio codecs
        this.pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        this.rtpFormats = new RTPFormats(1);
        this.rtpFormats.add(new RTPFormat(8, pcma, 8000));
    }

    @Before
    public void setUp() throws Exception {
        // Activate the core elements
        this.scheduler.start();
        this.udpManager.start();

        // Create media sources
        this.source1 = new Sine(scheduler);
        this.source1.setFrequency(100);
        this.source2 = new Sine(scheduler);
        this.source2.setFrequency(50);

        // Create media sinks
        this.analyzer1 = new SpectraAnalyzer("analyzer-1", scheduler);
        this.analyzer2 = new SpectraAnalyzer("analyzer-2", scheduler);

        InbandComponent sineComponent1 = new InbandComponent(8888, dspFactory.newProcessor());
        sineComponent1.addInput(source1.getMediaInput());
        sineComponent1.addOutput(analyzer1.getMediaOutput());
        sineComponent1.setReadable(true);
        sineComponent1.setWritable(true);

        InbandComponent sineComponent2 = new InbandComponent(8889, dspFactory.newProcessor());
        sineComponent2.addInput(source2.getMediaInput());
        sineComponent2.addOutput(analyzer2.getMediaOutput());
        sineComponent2.setReadable(true);
        sineComponent2.setWritable(true);

        // Create media mixers
        this.audioMixer1 = new AudioMixer(scheduler);
        this.audioMixer2 = new AudioMixer(scheduler);

        // Create media channel 1
        this.rtpClock1 = new RtpClock(scheduler.getClock());
        this.oobClock1 = new RtpClock(scheduler.getClock());
        this.statistics1 = new RtpStatistics(rtpClock1);
        this.channel1 = new RtpTransport(statistics1, scheduler, udpManager);
        this.channel1.bind(false);

        // Create mixer component for channel 1
        this.rtpComponent1 = new RtpComponent(CHANNEL1_ID, scheduler, channel1, rtpClock1, oobClock1, dspFactory.newProcessor());
        // XXX this.mixerComponent1.setRtpFormats(rtpFormats);
        this.channel1.setRtpRelay(rtpComponent1);

        // this.mixerComponent1.addInput(source1.getMediaInput());
        // this.mixerComponent1.addOutput(analyzer1.getMediaOutput());
        this.audioMixer1.addComponent(rtpComponent1.getInbandComponent());
        this.audioMixer1.addComponent(sineComponent1);

        // Create media channel 2
        this.rtpClock2 = new RtpClock(scheduler.getClock());
        this.oobClock2 = new RtpClock(scheduler.getClock());
        this.statistics2 = new RtpStatistics(rtpClock2);
        this.channel2 = new RtpTransport(statistics2, scheduler, udpManager);
        this.channel2.bind(false);

        // Create mixer component for channel 2
        this.rtpComponent2 = new RtpComponent(CHANNEL2_ID, scheduler, channel2, rtpClock2, oobClock2, dspFactory.newProcessor());
        // XXX this.mixerComponent2.setRtpFormats(rtpFormats);
        this.channel2.setRtpRelay(rtpComponent2);

        // this.mixerComponent2.addInput(source2.getMediaInput());
        // this.mixerComponent2.addOutput(analyzer2.getMediaOutput());
        this.audioMixer2.addComponent(rtpComponent2.getInbandComponent());
        this.audioMixer2.addComponent(sineComponent2);

        // Connect both media channels
        this.channel1.setFormatMap(rtpFormats);
        this.channel2.setFormatMap(rtpFormats);
        this.channel1.updateMode(ConnectionMode.SEND_RECV);
        this.channel2.updateMode(ConnectionMode.SEND_RECV);
        channel1.setRemotePeer(new InetSocketAddress("127.0.0.1", channel2.getLocalPort()));
        channel2.setRemotePeer(new InetSocketAddress("127.0.0.1", channel1.getLocalPort()));
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

        int s1[] = analyzer1.getSpectra();
        int s2[] = analyzer2.getSpectra();

        System.out.println("rx-channel1: " + channel1.getPacketsReceived());
        System.out.println("tx-channel1: " + channel1.getPacketsTransmitted());

        System.out.println("rx-channel2: " + channel2.getPacketsReceived());
        System.out.println("tx-channel2: " + channel2.getPacketsTransmitted());

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
        analyzer2.activate();
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

        assertEquals(1, s1.length);
        assertEquals(0, s2.length);
        assertEquals(50, s1[0], 5);
    }

    @Test
    public void testFailureRate() throws Exception {
        for (int i = 0; i < 1; i++) {
            System.out.println("Test# " + i);
            this.testTransmission();
        }
    }

}
