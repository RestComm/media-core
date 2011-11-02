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

package org.mobicents.javax.media.mscontrol.mediagroup.signals.buffer;

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
public class EventBufferTest implements BufferListener {
    
    private EventBuffer buffer = new EventBuffer();
    
    private boolean activatedOnCount = false;
    private boolean activatedOnPattern = false;
    private int patternIndex;
    private String s;
    
    public EventBufferTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        buffer.setListener(this);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of offer method, of class EventBuffer.
     */
    @Test
    public void testActivationOnCount() {
        this.activatedOnCount = false;
        buffer.setCount(1);
        buffer.offer(new Event("1"));
        
        assertTrue("Must be activated on count", this.activatedOnCount);
        assertEquals("1", s);
        assertEquals(0, buffer.length());
        
        this.activatedOnCount = false;
        
        buffer.setCount(3);
        buffer.offer(new Event("1"));
        buffer.offer(new Event("2"));
        buffer.offer(new Event("3"));
        
        assertTrue("Must be activated on count", this.activatedOnCount);
        assertEquals("123", s);
        assertEquals(0, buffer.length());
        
    }


    /**
     * Test of offer method, of class EventBuffer.
     */
    @Test
    public void testActivationOnPattern() {
        this.activatedOnPattern = false;
        
        buffer.setPatterns(new String[]{"123", "456"});
        
        buffer.offer(new Event("1"));
        buffer.offer(new Event("2"));
        buffer.offer(new Event("3"));
        
        assertTrue("Must be activated on pattern", this.activatedOnPattern);
        assertEquals("123", s);
        assertEquals(0, buffer.length());
        
        this.activatedOnPattern = false;
        
        buffer.setCount(3);
        buffer.offer(new Event("4"));
        buffer.offer(new Event("5"));
        buffer.offer(new Event("6"));
        
        assertTrue("Must be activated on pattern", this.activatedOnPattern);
        assertEquals("456", s);
        assertEquals(0, buffer.length());
        
    }
    
    public void patternMatches(int index, String s) {
        this.patternIndex = index;
        this.activatedOnPattern = true;
        this.s = s;
    }

    public void countMatches(String s) {
        this.activatedOnCount = true;
        this.s = s;
    }

    /**
     * Test of offer method, of class EventBuffer.
     */
    @Test
    public void testMatch() {
        this.activatedOnCount = false;
        
        buffer.setPatterns(new String[]{"#"});
        buffer.setCount(4);
        
        buffer.offer(new Event("1"));
        buffer.offer(new Event("2"));
        buffer.offer(new Event("3"));
        buffer.offer(new Event("4"));
        
        assertTrue("Must be activated on count", this.activatedOnCount);
        assertEquals("1234", s);
        assertEquals(0, buffer.length());
        
    }
    
}