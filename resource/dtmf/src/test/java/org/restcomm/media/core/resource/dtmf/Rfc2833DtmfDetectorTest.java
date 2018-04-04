/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
 * by the @authors tag.
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

package org.restcomm.media.core.resource.dtmf;

import net.ripe.hadoop.pcap.packet.Packet;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.core.pcap.GenericPcapReader;
import org.restcomm.media.core.pcap.PcapFile;
import org.restcomm.media.core.resource.dtmf.DtmfEvent;
import org.restcomm.media.core.resource.dtmf.DtmfEventObserver;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/**
 * @author yulian oifa
 * @author Vladimir Morosev (vladimir.morosev@telestax.com)
 */
public class Rfc2833DtmfDetectorTest {

    private static final Logger log = Logger.getLogger(Rfc2833DtmfDetectorTest.class);

    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() {
        scheduler = Executors.newScheduledThreadPool(1);
    }

    @After
    public void tearDown() {
        scheduler.shutdown();
    }

    @Test
    public void testDtmf4DigitsFast() throws InterruptedException {
        // given
        final int duration = 2000;
        final DtmfEventObserver observer = mock(DtmfEventObserver.class);
        final Rfc2833DtmfDetector detector = new Rfc2833DtmfDetector(200);
        detector.observe(observer);

        // when
        playDtmfPcapFile("/dtmf_rfc2833_4_digits_fast.pcap", detector);

        // then
        ArgumentCaptor<DtmfEvent> argument = ArgumentCaptor.forClass(DtmfEvent.class);
        verify(observer, timeout(duration).times(4)).onDtmfEvent(argument.capture());
        List<DtmfEvent> capturedEvents = argument.getAllValues();
        assertEquals("1", capturedEvents.get(0).getTone());
        assertEquals("2", capturedEvents.get(1).getTone());
        assertEquals("3", capturedEvents.get(2).getTone());
        assertEquals("4", capturedEvents.get(3).getTone());

        detector.forget(observer);
    }

    @Test
    public void testDtmf4DigitsSlow() throws InterruptedException {
        // given
        final int duration = 6400;
        final DtmfEventObserver observer = mock(DtmfEventObserver.class);
        final Rfc2833DtmfDetector detector = new Rfc2833DtmfDetector(500);
        detector.observe(observer);

        // when
        playDtmfPcapFile("/dtmf_rfc2833_4_digits_slow.pcap", detector);

        // then
        ArgumentCaptor<DtmfEvent> argument = ArgumentCaptor.forClass(DtmfEvent.class);
        verify(observer, timeout(duration).times(4)).onDtmfEvent(argument.capture());
        List<DtmfEvent> capturedEvents = argument.getAllValues();
        assertEquals("1", capturedEvents.get(0).getTone());
        assertEquals("2", capturedEvents.get(1).getTone());
        assertEquals("3", capturedEvents.get(2).getTone());
        assertEquals("4", capturedEvents.get(3).getTone());

        detector.forget(observer);
    }

    @Test
    public void testDtmf2DigitPairs() throws InterruptedException {
        // given
        final int duration = 4100;
        final DtmfEventObserver observer = mock(DtmfEventObserver.class);
        final Rfc2833DtmfDetector detector = new Rfc2833DtmfDetector(200);
        detector.observe(observer);

        // when
        playDtmfPcapFile("/dtmf_rfc2833_2_digit_pairs.pcap", detector);

        // then
        ArgumentCaptor<DtmfEvent> argument = ArgumentCaptor.forClass(DtmfEvent.class);
        verify(observer, timeout(duration).times(4)).onDtmfEvent(argument.capture());
        List<DtmfEvent> capturedEvents = argument.getAllValues();
        assertEquals("1", capturedEvents.get(0).getTone());
        assertEquals("1", capturedEvents.get(1).getTone());
        assertEquals("2", capturedEvents.get(2).getTone());
        assertEquals("2", capturedEvents.get(3).getTone());

        detector.forget(observer);
    }

    public void playDtmfPcapFile(String resourceName, Rfc2833DtmfDetector detector) {
        final URL inputFileUrl = this.getClass().getResource(resourceName);
        PcapFile pcap = new PcapFile(inputFileUrl);
        try {
            pcap.open();
            scheduler.schedule(new PlayPacketTask(pcap, detector, null, 0, 0.0), 0, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            log.error("Could not read file", e);
            fail("DTMF tone detector test file access error");
        }
    }

    private class PlayPacketTask implements Runnable {

        private PcapFile pcap;
        private Rfc2833DtmfDetector detector;
        private byte[] lastPacketRtpPayload;
        private int lastPacketDuration;
        private double lastPacketTimestamp;

        public PlayPacketTask(PcapFile pcap, Rfc2833DtmfDetector detector, byte[] rtpPayload, int duration, double timestamp) {
            this.pcap = pcap;
            this.detector = detector;
            this.lastPacketRtpPayload = rtpPayload;
            this.lastPacketDuration = duration;
            this.lastPacketTimestamp = timestamp;
        }

        public void run() {
            if (lastPacketRtpPayload != null)
                detector.detect(lastPacketRtpPayload, lastPacketDuration);
            if (!pcap.isComplete()) {
                final Packet packet = pcap.read();
                byte[] payload = (byte[]) packet.get(GenericPcapReader.PAYLOAD);

                byte[] rtpPayload = Arrays.copyOfRange(payload, 12, payload.length);;

                double timestamp = (double) packet.get(Packet.TIMESTAMP_USEC);
                int duration;
                if (lastPacketTimestamp == 0.0)
                    duration = 20;
                else
                    duration = (int) ((timestamp - lastPacketTimestamp) * 1000);

                scheduler.schedule(new PlayPacketTask(pcap, detector, rtpPayload, duration, timestamp), duration, TimeUnit.MILLISECONDS);
            } else {
                try {
                    pcap.close();
                } catch (IOException e) {
                    log.error("Could not read file", e);
                    fail("DTMF tone detector test file access error");
                }
            }
        }
    }
}
