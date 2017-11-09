/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.asr;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.spi.memory.Frame;

/**
 * @author anikiforov
 */
public class SpeechDetectorImplTest {

    private SpeechDetectorImpl testee;
    private SpeechDetectorCounter listener;

    @Before
    public void setUp() {
        testee = new SpeechDetectorImpl("SpeechDetectorImplTest", 10);
        listener = new SpeechDetectorCounter();

        testee.startSpeechDetection(listener);
        testee.start();
    }

    @After
    public void cleanUp() {
        testee.stop();
        testee.stopSpeechDetection();

        listener = null;
        testee = null;
    }

    @Test
    public void testSilenceFrame() {
        assertTrue(testee.checkForSilence(new TestSilenceFrame()));
    }

    @Test
    public void testSpeechFrame() {
        assertFalse(testee.checkForSilence(new TestSpeechFrame()));
    }

    @Test
    public void testSilence() {
        final int FRAME_COUNT = 100;
        for (int i = 0; i < FRAME_COUNT; i++) {
            sendSilenceFrame();
        }
        assertEquals(0, listener.getSpeechDetectedCounter());
    }

    @Test
    public void testSpeech() {
        final int FRAME_COUNT = 100;
        for (int i = 0; i < FRAME_COUNT; i++) {
            sendSpeechFrame();
        }
        assertEquals(FRAME_COUNT, listener.getSpeechDetectedCounter());
    }

    @Test
    public void testSpeechAndSilence() {
        final int FRAME_COUNT = 100;
        for (int i = 0; i < FRAME_COUNT; i++) {
            sendSpeechFrame();
            sendSilenceFrame();
        }
        assertEquals(FRAME_COUNT, listener.getSpeechDetectedCounter());
    }

    private static class SpeechDetectorCounter implements SpeechDetectorListener {
        private int speechDetectedCounter = 0;

        @Override
        public void onSpeechDetected() {
            speechDetectedCounter++;
        }

        public int getSpeechDetectedCounter() {
            return speechDetectedCounter;
        }
    }

    private static class TestSilenceFrame extends Frame {
        private static final byte[] BYTES = new byte[] { 0x00, 0x00, 0x00, 0x00 };

        public TestSilenceFrame() {
            super(null, BYTES);
            setLength(BYTES.length);
        }
    }

    private static class TestSpeechFrame extends Frame {
        private static final byte[] BYTES = new byte[] { 0x00, 0x0f, 0x70, 0x7f };

        public TestSpeechFrame() {
            super(null, BYTES);
            setLength(BYTES.length);
        }
    }

    private void sendSpeechFrame() {
        testee.perform(new TestSpeechFrame());
    }

    private void sendSilenceFrame() {
        testee.perform(new TestSilenceFrame());
    }

}