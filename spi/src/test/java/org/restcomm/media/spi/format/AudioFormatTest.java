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

package org.restcomm.media.spi.format;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.spi.format.AudioFormat;
import org.restcomm.media.spi.format.EncodingName;
import org.restcomm.media.spi.format.FormatFactory;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class AudioFormatTest {

    public AudioFormatTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSampleRate method, of class AudioFormat.
     */
    @Test
    public void testCloneExecutionTime() {
        AudioFormat f = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);

        long s = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            f.clone();
        }
        long duration = System.currentTimeMillis() - s;
        System.out.println("Execution time: " + duration);
        assertTrue("To slow", 3000000L > duration);
    }

    @Test
    public void testImmutable() {
        AudioFormat f = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
        AudioFormat fc = f.clone();

        //changing encoding name of the clone, original name must remains same
        fc.setName(new EncodingName("pcma"));
        assertEquals("pcmu", f.getName().toString());

        //changing source and checking again
        f.setName(new EncodingName("gsm"));
        assertEquals("pcma", fc.getName().toString());
    }

    @Test
    public void testMatch() {
        AudioFormat f1 = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
        AudioFormat f2 = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);

        assertTrue("Formats must match", f1.matches(f2));
    }

}
