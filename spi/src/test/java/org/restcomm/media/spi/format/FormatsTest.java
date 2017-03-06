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
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.format.Formats;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class FormatsTest {

    public FormatsTest() {
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
     * Test of intersection method, of class Formats.
     */
    @Test
    public void testIntersection() {
        Formats f1 = new Formats();
        Formats f2 = new Formats();
        Formats f3 = new Formats();

        AudioFormat pcma = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
        AudioFormat linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

        f1.add(pcma);
        f1.add(linear);

        f2.add(pcma);

        f1.intersection(f2, f3);

        assertEquals(1, f3.size());
        assertTrue("pcma expected", pcma.matches(f3.get(0)));
    }

}