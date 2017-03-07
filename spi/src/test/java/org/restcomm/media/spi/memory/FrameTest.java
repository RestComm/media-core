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

package org.restcomm.media.spi.memory;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.spi.format.FormatFactory;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class FrameTest {

    public FrameTest() {
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
     * Test of reset method, of class Frame.
     */
    @Test
    public void testClone() {
    	Frame frame1 = Memory.allocate(3);
        
        frame1.getData()[0] = 1;
        frame1.getData()[1] = 2;
        frame1.getData()[2] = 3;

        frame1.setDuration(20);
        frame1.setOffset(0);
        frame1.setLength(3);
        frame1.setTimestamp(123);
        frame1.setFormat(FormatFactory.createAudioFormat("LINEAR", 8000));

        Frame frame2 = frame1.clone();
        assertEquals(frame2.getDuration(), frame1.getDuration());
        assertEquals(frame2.getOffset(), frame1.getOffset());
        assertEquals(frame2.getLength(), frame1.getLength());
        assertEquals(frame2.getTimestamp(), frame1.getTimestamp());
        assertEquals("LINEAR", frame2.getFormat().getName().toString());

        assertEquals(frame1.getData()[0], frame2.getData()[0]);
        assertEquals(frame1.getData()[1], frame2.getData()[1]);
        assertEquals(frame1.getData()[2], frame2.getData()[2]);

        //change frame2 and check that frame1 remains constant
        frame2.getData()[1] = 100;
        assertEquals(2, (int)(frame1.getData()[1]));    	
    }

}