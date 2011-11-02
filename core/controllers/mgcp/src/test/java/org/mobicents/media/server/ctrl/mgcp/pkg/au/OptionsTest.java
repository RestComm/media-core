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

package org.mobicents.media.server.ctrl.mgcp.pkg.au;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class OptionsTest {

    private String s = "an=file du=1000 of=10 iv=20 ip=xxx dp=123|456 ni=true";
    private Options params;
    
    public OptionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        params = new Options(s);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getUri method, of class Options.
     */
    @Test
    public void testGetFirstSegments() {
        assertEquals("file", params.next());
    }

    @Test
    public void testSegments() {
        String s1 = "an=(file1;file2;file3)";
        Options params = new Options(s1);
        assertEquals("file1", params.next());
        assertTrue("Two segments remain", params.hasMoreSegments());
        assertEquals("file2", params.next());
        assertTrue("One segments remain", params.hasMoreSegments());
        assertEquals("file3", params.next());
        assertFalse("Zero segments remain", params.hasMoreSegments());
    }

    /**
     * Test of getDuration method, of class Options.
     */
    @Test
    public void testGetDuration() {
        assertEquals(1000, params.getDuration());
    }

    /**
     * Test of getOffset method, of class Options.
     */
    @Test
    public void testGetOffset() {
        assertEquals(10, params.getOffset());
    }

    @Test
    public void testGetInterval() {
        assertEquals(20, params.getInterval());
    }

    @Test
    public void testPrompt() {
        assertEquals("xxx", params.getPrompt());
    }

    @Test
    public void testPatterns() {
        String[] dp = params.getDigitPattern();
        assertEquals("123", dp[0]);
        assertEquals("456", dp[1]);
    }

    @Test
    public void testNonInterruptablePlay() {
        assertTrue("Non interruptable play expected", params.isNonInterruptablePlay());
    }
    
}