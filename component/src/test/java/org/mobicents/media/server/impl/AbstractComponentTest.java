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

package org.mobicents.media.server.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.memory.Frame;
/**
 *
 * @author yulian oifa
 */
public class AbstractComponentTest {

    private TestSource src;
    private TestSink sink;
    
    private Clock clock;
    private Scheduler scheduler;
    
    public AbstractComponentTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    	clock = new DefaultClock();
    	
    	scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();    
        
        src = new TestSource("source",scheduler);
        sink = new TestSink("sink");
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testSource() {
    	src.connect(sink);                
        assertTrue("Source still not connected", src.isConnected());

        src.disconnect();
        assertFalse("Source remains connected", src.isConnected());
    }    

    private class TestSource extends AbstractSource {

		private static final long serialVersionUID = 3317111509574993827L;

		public TestSource(String name,Scheduler scheduler) {
            super(name, scheduler, Scheduler.OUTPUT_QUEUE);
        }

        @Override
        public Frame evolve(long timestamp) {
            return null;
        }
    }

    private class TestSink extends AbstractSink {
    	
		private static final long serialVersionUID = 5713559743426595813L;

		public TestSink(String name) {
            super(name);
        }

        @Override
        public void onMediaTransfer(Frame frame) {
        }

        @Override
        public void deactivate() {        	
        }

        @Override
        public void activate() {
        }
    }

}
