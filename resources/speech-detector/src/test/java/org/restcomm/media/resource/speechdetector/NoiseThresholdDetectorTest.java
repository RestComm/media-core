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

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
  * @author Vladimir Morosev (vladimir.morosev@telestax.com)
  */
public class NoiseThresholdDetectorTest {

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
    	final byte[] bytes = new byte[] { 0x00, 0x00, 0x00, 0x00 };
        assertTrue(testee.detect(bytes, 0, bytes.length));
    }

    @Test
    public void testSpeech() {
    	final byte[] bytes = new byte[] { 0x00, 0x0f, 0x70, 0x7f };
        assertFalse(testee.detect(bytes, 0, bytes.length));
    }

}
