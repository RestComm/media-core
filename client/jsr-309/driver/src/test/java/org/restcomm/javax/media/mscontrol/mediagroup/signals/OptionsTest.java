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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.javax.media.mscontrol.mediagroup.signals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.javax.media.mscontrol.mediagroup.signals.Options;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class OptionsTest {

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of hasMoreSegments method, of class Options.
     */
    @Test
    public void testNumDigitsAndPatterns() {
        Options options = new Options();
        options.setDigitsNumber(1);
        options.setDigitPattern(new String[] {"123", "567"});
        assertEquals("mn=1 dp=123|567", options.toString());
    }

    @Test
    public void testNegativeNumDigitsAndPatterns() {
        Options options = new Options();
        options.setDigitsNumber(-1);
        options.setDigitPattern(new String[] {"123", "567"});
        assertEquals("dp=123|567", options.toString());
    }

    @Test
    public void testNumDigitsOnly() {
        Options options = new Options();
        options.setDigitsNumber(1);
        assertEquals("mn=1", options.toString());
    }

    @Test
    public void testNonInterruptable() {
        Options options = new Options();
        options.setNonInterruptiblePlay(true);
        assertEquals("ni=true", options.toString());
    }
    
}
