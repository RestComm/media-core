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

package org.mobicents.media.server.component;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 * @author kulikov
 */
public class ElasticBufferTest implements BufferListener {

    private final static int delay = 3;
    private final static int limit = 10;

    private ElasticBuffer buffer;

    private boolean ready;

    public ElasticBufferTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        buffer = new ElasticBuffer(delay, limit);
        buffer.setListener(this);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setListener method, of class ElasticBuffer.
     */
    @Test
    public void testFIFO() {
        int n = 10000;
        for (int i = 0; i < n; i++) {
            Frame frame = Memory.allocate(10);
            frame.setSequenceNumber(i);

            buffer.write(frame);

            Frame frame1 = buffer.read();
            if (i < delay) {
                assertTrue("Expected null", frame1 == null);
            } else {
                assertEquals(i - delay, frame1.getSequenceNumber());
                frame1.recycle();
            }

        }
    }

    @Test
    public void testState() {
        for (int i = 0; i <= delay; i++) {
            buffer.write(Memory.allocate(10));
        }

        assertTrue("Buffer must be ready", ready);

        //empty buffer
        for (int i = 0; i <= delay; i++) {
            buffer.read().recycle();
        }

        assertTrue("Buffer empty and not ready", buffer.read() == null);
        ready = false;

        buffer.write(Memory.allocate(10));
        assertTrue("Buffer still not ready", buffer.read() == null);

        for (int i = 0; i < delay; i++) {
            buffer.write(Memory.allocate(10));
        }
        assertTrue("Buffer ready again", ready);
    }

    @Test
    public void testOverflow() {
        int count = limit + limit / 2;
        for (int i = 0; i < count; i++) {
            Frame frame = Memory.allocate(10);
            frame.setSequenceNumber(i);

            buffer.write(frame);
        }

        Frame f = buffer.read();
        assertEquals(5, f.getSequenceNumber());

    }

    public void onReady() {
        this.ready = true;
    }

    @Test
    public void testPerformance() {
        long s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            buffer.write(Memory.allocate(10));
        }

        long f = System.nanoTime();
        System.out.println("Duration = " + (f-s));

        ConcurrentLinkedQueue<Frame> queue = new ConcurrentLinkedQueue();
        s = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            queue.offer(Memory.allocate(10));
        }

        f = System.nanoTime();
        System.out.println("Duration = " + (f-s));

    }
}