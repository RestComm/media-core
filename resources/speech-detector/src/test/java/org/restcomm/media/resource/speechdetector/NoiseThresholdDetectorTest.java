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

package org.restcomm.media.resource.speechdetector;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/**
  * @author Vladimir Morosev (vladimir.morosev@telestax.com)
  */
public class NoiseThresholdDetectorTest {

    private static final Logger log = Logger.getLogger(NoiseThresholdDetectorTest.class);

    private SpeechDetector testee;

    @Before
    public void setUp() {
        testee = new NoiseThresholdDetector(10);
    }

    @After
    public void cleanUp() {
        testee = null;
    }

    @Test
    public void testSilence() {

        // given
        boolean testPassed = true;
        URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48_silence.pcm");

        // when
        try {
            final int packetSize = 480;
            try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile())) {
                byte[] input = new byte[2 * packetSize];
                while (inputStream.read(input) == 2 * packetSize) {
                    if (testee.detect(input, 0, input.length)) {
                        testPassed = false;
                        break;
                    }
                }
            } finally {
            }
        } catch (IOException exc) {
            log.error("IOException: " + exc.getMessage());
            fail("Speech Detector test file access error");
        }

        // then
        assertTrue(testPassed);
    }

    @Test
    public void testSpeech() {

        // given
        boolean testPassed = false;
        URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48_speech.pcm");

        // when
        try {
            final int packetSize = 480;
            try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile())) {
                byte[] input = new byte[2 * packetSize];
                while (inputStream.read(input) == 2 * packetSize) {
                    if (testee.detect(input, 0, input.length)) {
                        testPassed = true;
                    }
                }
            } finally {
            }
        } catch (IOException exc) {
            log.error("IOException: " + exc.getMessage());
            fail("Speech Detector test file access error");
        }

        // then
        assertTrue(testPassed);
    }

}
