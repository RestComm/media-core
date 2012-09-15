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

package org.mobicents.javax.media.mscontrol.mediagroup.signals;

import javax.media.mscontrol.Parameter;
import javax.media.mscontrol.Parameters;
import javax.media.mscontrol.mediagroup.signals.SignalDetector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.javax.media.mscontrol.ParametersImpl;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class PatternsTest {

    private Patterns patterns;
    
    public PatternsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        Parameters params = new ParametersImpl();
        params.put(SignalDetector.PATTERN[1], "123");
        params.put(SignalDetector.PATTERN[2], "3456");
        
        patterns = new Patterns(new Parameter[]{SignalDetector.PATTERN[2], SignalDetector.PATTERN[1]}, params);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of hasMore method, of class Patterns.
     */
    @Test
    public void testPatterns() {
        assertTrue("Should not be empty", patterns.hasMore());
        assertEquals("123", patterns.next().getValue());
        assertTrue("Should not be empty", patterns.hasMore());
        assertEquals("3456", patterns.next().getValue());
        assertFalse("Should be empty", patterns.hasMore());
    }

}