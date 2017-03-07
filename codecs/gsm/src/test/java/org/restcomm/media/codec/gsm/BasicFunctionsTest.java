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

package org.restcomm.media.codec.gsm;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.codec.gsm.BasicFunctions;

import static org.junit.Assert.*;

/**
 *
 * @author oifa yulian
 */
public class BasicFunctionsTest {
	
	public BasicFunctionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {        
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDiv() {    	
    	assertEquals(Short.MAX_VALUE, BasicFunctions.div((short)100,(short)100));        
        
        assertEquals(8192, BasicFunctions.div((short)100,(short)400));
    }

    @Test
    public void testNorm() {    
    	//positive values
    	assertEquals(0, BasicFunctions.norm(0x40000000));

        assertEquals(8, BasicFunctions.norm(0x00400000));
        
        assertEquals(17, BasicFunctions.norm(0x00002000));
        
        assertEquals(26, BasicFunctions.norm(0x00000010));
        
        //negative values
        assertEquals(2, BasicFunctions.norm(0xF0000000));

        assertEquals(7, BasicFunctions.norm(0xFF800000));
        
        assertEquals(16, BasicFunctions.norm(0xFFFFC000));
        
        assertEquals(25, BasicFunctions.norm(0xFFFFFFE0));
    }
}
