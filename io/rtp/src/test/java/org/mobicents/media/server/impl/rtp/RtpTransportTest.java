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
import org.mobicents.media.server.impl.rtp.channels.AudioSession;
import org.mobicents.media.server.impl.rtp.channels.RtpSession;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
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

    // Core elements
    private final Scheduler scheduler;
    private final UdpManager udpManager;
    private final DspFactoryImpl dspFactory;
    private final ChannelsManager channelsManager;
    private final RTPFormats formats;

    // Runtime elements
    SineComponent component1, component2;
    AudioMixer mixer1, mixer2;
    RtpSession session1, session2;

    public RtpTransportTest() throws IOException {
        // Scheduler
        this.scheduler = new Scheduler();
        this.scheduler.setClock(new DefaultClock());

        // Network
        this.udpManager = new UdpManager(scheduler);

        // Transcoder
        this.dspFactory = new DspFactoryImpl();
        this.dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Encoder");
        this.dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.alaw.Decoder");

        // Media Sessions
        this.channelsManager = new ChannelsManager(udpManager, dspFactory);
        this.channelsManager.setScheduler(scheduler);

        // Codecs
        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        this.formats = new RTPFormats(1);
        this.formats.add(new RTPFormat(8, pcma, 8000));
    }

    private class SineComponent extends InbandComponent {

        private final Sine sine;
        private final SpectraAnalyzer analyzer;

        public SineComponent(int componentId, int frequency, boolean readable, boolean writable) {
            super(componentId, dspFactory.newProcessor());
            this.sine = new Sine(scheduler);
            this.sine.setFrequency(frequency);
            this.analyzer = new SpectraAnalyzer("analyzer" + componentId, scheduler);
            addInput(sine.getMediaInput());
            addOutput(analyzer.getMediaOutput());
            setReadable(readable);
            setWritable(writable);
        }

        public Sine getSine() {
            return sine;
        }

        public SpectraAnalyzer getAnalyzer() {
            return analyzer;
        }

    }

    @Before
    public void before() {
        this.scheduler.start();
        this.udpManager.start();
    }

    @After
    public void after() {
        this.component1.getSine().deactivate();
        this.component1.getAnalyzer().deactivate();
        this.session1.close();
        this.mixer1.stop();

        this.component2.getSine().deactivate();
        this.component2.getAnalyzer().deactivate();
        this.session2.close();
        this.mixer2.stop();

        this.udpManager.stop();
        this.scheduler.stop();
    }

    @Test
    public void testFullDuplex() throws Exception {
        // Given
        this.component1 = new SineComponent(1, 100, true, true);
        this.session1 = new AudioSession(9999, this.scheduler, this.dspFactory.newProcessor(), this.udpManager);
        this.session1.negotiateFormats(this.formats);
        this.session1.bind(false, false);
        this.session1.open();

        this.mixer1 = new AudioMixer(this.scheduler);
        this.mixer1.addComponent(component1);
        this.mixer1.addComponent(session1.getMediaComponent().getInbandComponent());

        this.component2 = new SineComponent(2, 50, true, true);
        this.session2 = new AudioSession(9998, this.scheduler, this.dspFactory.newProcessor(), this.udpManager);
        this.session2.negotiateFormats(this.formats);
        this.session2.bind(false, false);
        this.session2.open();

        this.mixer2 = new AudioMixer(this.scheduler);
        this.mixer2.addComponent(component2);
        this.mixer2.addComponent(session2.getMediaComponent().getInbandComponent());

        // When
        this.session1.setConnectionMode(ConnectionMode.SEND_RECV);
        this.session2.setConnectionMode(ConnectionMode.SEND_RECV);

        this.session1.connectRtp(new InetSocketAddress("127.0.0.1", session2.getRtpPort()));
        this.session2.connectRtp(new InetSocketAddress("127.0.0.1", session1.getRtpPort()));

        this.component1.getSine().activate();
        this.component1.getAnalyzer().activate();
        this.mixer1.start();

        this.component2.getSine().activate();
        this.component2.getAnalyzer().activate();
        this.mixer2.start();

        Thread.sleep(5000);

        this.component1.getSine().deactivate();
        this.component1.getAnalyzer().deactivate();
        this.mixer1.stop();

        this.component2.getSine().deactivate();
        this.component2.getAnalyzer().deactivate();
        this.mixer2.stop();

        // Then
        int s1[] = this.component1.getAnalyzer().getSpectra();
        int s2[] = this.component2.getAnalyzer().getSpectra();

        System.out.println("rx-channel1: " + session1.getPacketsReceived());
        System.out.println("tx-channel1: " + session1.getPacketsSent());
        System.out.println("rx-channel2: " + session2.getPacketsReceived());
        System.out.println("tx-channel2: " + session2.getPacketsSent());

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);
        assertEquals(50, s1[0], 5);
        assertEquals(100, s2[0], 5);
    }

    @Test
    public void testHalfDuplex() throws Exception {
        // Given
        this.component1 = new SineComponent(1, 100, true, true);
        this.session1 = new AudioSession(9999, this.scheduler, this.dspFactory.newProcessor(), this.udpManager);
        this.session1.negotiateFormats(this.formats);
        this.session1.bind(false, false);
        this.session1.open();

        this.mixer1 = new AudioMixer(this.scheduler);
        this.mixer1.addComponent(component1);
        this.mixer1.addComponent(session1.getMediaComponent().getInbandComponent());

        this.component2 = new SineComponent(2, 50, true, true);
        this.session2 = new AudioSession(9998, this.scheduler, this.dspFactory.newProcessor(), this.udpManager);
        this.session2.negotiateFormats(this.formats);
        this.session2.bind(false, false);
        this.session2.open();

        this.mixer2 = new AudioMixer(this.scheduler);
        this.mixer2.addComponent(component2);
        this.mixer2.addComponent(session2.getMediaComponent().getInbandComponent());

        // When
        this.session1.setConnectionMode(ConnectionMode.SEND_ONLY);
        this.session2.setConnectionMode(ConnectionMode.RECV_ONLY);

        this.session1.connectRtp(new InetSocketAddress("127.0.0.1", session2.getRtpPort()));
        this.session2.connectRtp(new InetSocketAddress("127.0.0.1", session1.getRtpPort()));

        this.component1.getSine().activate();
        this.component1.getAnalyzer().activate();
        this.mixer1.start();

        this.component2.getSine().activate();
        this.component2.getAnalyzer().activate();
        this.mixer2.start();

        Thread.sleep(5000);

        this.component1.getSine().deactivate();
        this.component1.getAnalyzer().deactivate();
        this.mixer1.stop();

        this.component2.getSine().deactivate();
        this.component2.getAnalyzer().deactivate();
        this.mixer2.stop();

        // Then
        int s1[] = this.component1.getAnalyzer().getSpectra();
        int s2[] = this.component2.getAnalyzer().getSpectra();

        System.out.println("rx-channel1: " + session1.getPacketsReceived());
        System.out.println("tx-channel1: " + session1.getPacketsSent());
        System.out.println("rx-channel2: " + session2.getPacketsReceived());
        System.out.println("tx-channel2: " + session2.getPacketsSent());

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);
        assertEquals(100, s2[0], 5);
    }

    @Test
    public void testMultipleFullDuplex() throws Exception {
        for (int i = 0; i < 10; i++) {
            testFullDuplex();
        }
    }

}
