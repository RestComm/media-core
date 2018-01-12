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

import java.lang.String;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
  * @author Vladimir Morosev (vladimir.morosev@telestax.com)
  */
public class MovingThresholdDetectorTest {

    private static final Logger log = Logger.getLogger(MovingThresholdDetectorTest.class);

    private SpeechDetector getSpeechDetector() {
        List<SpeechDetector> detectors = SpeechDetector.getPluginManager().getExtensions(SpeechDetector.class);
        for (SpeechDetector detector : detectors) {
            return detector;
        }
	return null;
    }

    @Test
    public void testSilence() {
        // given
        final URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48_silence.pcm");
        final int packetSize = 480;
        SpeechDetector detector = getSpeechDetector();

        // when
        int framesDetected = 0;
        int framesCount = 0;
        try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile())) {
            byte[] input = new byte[2 * packetSize];
            while (inputStream.read(input) == 2 * packetSize) {
                framesCount++;
                if (detector.detect(input, 0, input.length))
                    framesDetected++;
            }
        } catch (IOException e) {
            log.error("Could not read file", e);
            fail("Speech Detector test file access error");
        }

        // then
        assertTrue(framesDetected / framesCount < 0.05);
    }

    @Test
    public void testNoiseOverSilenceThreshold() {
        // given
        final URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48_speech.pcm");
        final SpeechDetector detector = getSpeechDetector();

        // when
        int framesDetected = 0;
        int framesCount = 0;
        final int packetSize = 480;
        try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile())) {
            byte[] input = new byte[2 * packetSize];
            while (inputStream.read(input) == 2 * packetSize) {
                framesCount++;
                if (detector.detect(input, 0, input.length))
                    framesDetected++;
            }
        } catch (IOException exc) {
            log.error("IOException: " + exc.getMessage());
            fail("Speech Detector test file access error");
        }

        // then
        assertTrue((double)framesDetected / (double)framesCount >= 0.84);
    }

    @Test
    public void testNoiseUnderSilenceThreshold() {
        // given
        final URL inputFileUrl = this.getClass().getResource("/test_sound_mono_48_speech.pcm");
        final SpeechDetector detector = getSpeechDetector();

        // when
        int framesDetected = 0;
        int framesCount = 0;
        final int packetSize = 480;
        try (FileInputStream inputStream = new FileInputStream(inputFileUrl.getFile())) {
            byte[] input = new byte[2 * packetSize];
            while (inputStream.read(input) == 2 * packetSize) {
                framesCount++;
                if (detector.detect(input, 0, input.length))
                    framesDetected++;
            }
        } catch (IOException exc) {
            log.error("IOException: " + exc.getMessage());
            fail("Speech Detector test file access error");
        }

        // then
        assertFalse((double)framesDetected / (double)framesCount < 0.84);
    }

}
