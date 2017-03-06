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

package org.restcomm.media.component.audio;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.component.audio.Resampler;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class ResamplerTest {

    private final static int f = 8000;
    private final static int F = 8192;

    private double[] line = new double[8000];

    private final static double k = 10;

    private Resampler resampler = new Resampler(f,F);

    public ResamplerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        for (int i = 0; i < line.length; i++) {
            line[i] = k * (1/(double)f) * i;
        }

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of perform method, of class Resampler.
     */
    @Test
    public void testPerform() {
        double[] res = resampler.perform(line, line.length);
        assertEquals(8192, res.length);

        for (int i = 0; i < res.length - 1; i++) {
            double K = (res[i + 1] - res[i]) / (1./(double)F);
            assertTrue(Math.abs(K - k) < 0.1);
        }
    }

}