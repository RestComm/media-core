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

package org.mobicents.media.server.impl;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 *
 *
 * @author kulikov
 */
public class PipeImplTest {

    private final static AudioFormat fmt = FormatFactory.createAudioFormat(new EncodingName("LINEAR"));
    private final static Formats formats = new Formats();

    private Clock clock;
    private Scheduler scheduler;

    private PipeImpl pipe = new PipeImpl();
    private int count;

    private MyTestSource source;
    private MyTestSink sink;

    static {
        formats.add(fmt);
    }

    public PipeImplTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        source = new MyTestSource(scheduler);
        sink = new MyTestSink(scheduler);

        count = 0;
    }

    @After
    public void tearDown() {
        scheduler.stop();
    }

    @Test
    public void testTransmission() throws InterruptedException {
        pipe.connect(sink);
        pipe.connect(source);

        source.start();
        sink.start();

        Thread.sleep(5000);

        source.stop();
        sink.stop();

        assertTrue("Excpected at least 4 packets", count >= 4);
    }
    
    @Test
    public void testPipeStart() throws InterruptedException {
        pipe.connect(sink);
        pipe.connect(source);

        pipe.start();

        Thread.sleep(5000);

        pipe.stop();

        assertTrue("Excpected at least 4 packets", count >= 4);
    }

    private class MyTestSource extends AbstractSource {

        public MyTestSource(Scheduler scheduler) {
            super("test-source", scheduler, scheduler.SPLITTER_OUTPUT_QUEUE);
        }

        @Override
        public Frame evolve(long timestamp) {
            Frame frame = Memory.allocate(320);
            frame.setFormat(fmt);
            frame.setOffset(0);
            frame.setLength(320);
            frame.setDuration(1000000000L);
            return frame;
        }

        public Formats getNativeFormats() {
            return formats;
        }

    }

    private class MyTestSink extends AbstractSink {

        public MyTestSink(Scheduler scheduler) {
            super("test-sink", scheduler,scheduler.MIXER_INPUT_QUEUE);
        }

        @Override
        public void onMediaTransfer(Frame frame) throws IOException {
            count++;
            frame.recycle();
        }

        public Formats getNativeFormats() {
            return formats;
        }

    }
}