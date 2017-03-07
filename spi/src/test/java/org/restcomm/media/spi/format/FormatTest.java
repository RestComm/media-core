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
import org.restcomm.media.spi.format.EncodingName;
import org.restcomm.media.spi.format.Format;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class FormatTest {

    public FormatTest() {
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
     * Test of setName method, of class Format.
     */
    @Test
    public void testMatch() {
        Format f1 = new Format(new EncodingName("name1"));
        Format f2 = new Format(new EncodingName("Name1"));
        assertTrue("Formats must match", f1.matches(f2));
    }

    /**
     * Same as previous test except order
     */
    @Test
    public void testSimmetryMatch() {
        Format f1 = new Format(new EncodingName("name1"));
        Format f2 = new Format(new EncodingName("Name1"));
        assertTrue("Formats must match", f2.matches(f1));
    }

    @Test
    public void testNameChange() {
        Format f1 = new Format(new EncodingName("name1"));
        Format f2 = new Format(new EncodingName("Name1"));
        assertTrue("Formats must match", f1.matches(f2));
        f1.setName(new EncodingName("name2"));
        assertFalse("Formats must not match", f1.matches(f2));
    }


    @Test
    public void testComporatorExecutionTime() {
        Format f1 = new Format(new EncodingName("name1"));
        Format f2 = new Format(new EncodingName("Name1"));
        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            f1.matches(f2);
        }
        long f = System.nanoTime();
        System.out.println("Match execution time=" + (f-s));
    }

}