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

package org.restcomm.media.spi.listener;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.spi.listener.Event;
import org.restcomm.media.spi.listener.Listener;
import org.restcomm.media.spi.listener.Listeners;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class ListenersTest {
    private Listeners<TestListener> listeners = new Listeners<TestListener>();
    private TestEvent event;
    
    public ListenersTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        event = null;
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of add method, of class Listeners.
     */
    @Test
    public void testAdd() throws Exception {
    	listeners.add(new TestListener());
        listeners.dispatch(new TestEvent("test"));
        assertEquals("test", event.getName());        
    }

    /**
     * Test of remove method, of class Listeners.
     */
    @Test
    public void testRemove() throws Exception {
    	TestListener l = new TestListener();
        listeners.add(l);
        listeners.remove(l);
        listeners.dispatch(new TestEvent("test"));
        assertEquals(null, event);        
    }

    /**
     * Test of remove method, of class Listeners.
     */
    @Test
    public void testConcurrentRemove() throws Exception {
    	TestListener l = new TestListener();
        listeners.add(l);
        listeners.dispatch(new TestEvent("test"));
        assertEquals("test", event.getName());
        event = null;
        listeners.dispatch(new TestEvent("test"));
        assertEquals(null, event);        
    }
    
    private class TestListener implements Listener<TestEvent> {

        public void process(TestEvent evt) {            
            event = evt;
            System.out.println("Delivered " + evt);
            listeners.remove(this);
        }
        
    }
    
    private class TestEvent implements Event<TestEvent> {
        private String name;
        
        public TestEvent(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }

        public TestEvent getSource() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}