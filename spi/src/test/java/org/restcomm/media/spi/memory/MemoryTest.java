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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class MemoryTest {

    public MemoryTest() {
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
     * Test of allocate method, of class Memory.
     */
    @Test
    public void testAllocate() throws InterruptedException {
/*        Frame frame = Memory.allocate(160);
        frame.getData()[0] = 100;
        frame.setTimestamp(System.nanoTime());
        frame.setDuration(5000000000L);

        Thread.sleep(3000);
        
        Frame frame1 = Memory.allocate(160);
        assertEquals(0, frame1.getData()[0]);

        frame1.setTimestamp(System.nanoTime());
        frame1.setDuration(5000000000L);

        Thread.sleep(3000);
        Frame frame2 = Memory.allocate(160);
        assertEquals(100, frame2.getData()[0]);
*/
    }

    //@Test
    public void testGC() throws InterruptedException {
        for (int i = 0; i < 5000; i++) {
            Frame frame = Memory.allocate(160);
            //frame.setTimestamp(Memory.clock.getTime());
            frame.setDuration(20000000L);
            Thread.sleep(20);
        }
    }


}