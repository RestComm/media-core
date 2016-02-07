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

package org.mobicents.media.core.naming;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author yulian oifa
 */
public class EndpointNameGeneratorTest {

    private EndpointNameGenerator nameGenerator = new EndpointNameGenerator();

    public EndpointNameGeneratorTest() {
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
     * Test of setPattern method, of class EndpointNameGenerator.
     */
    @Test
    public void testSinglePattern() {
        String pattern = "mobicents/aap/[1..3]";
        nameGenerator.setPattern(pattern);

        assertTrue("Three names remains", nameGenerator.hasMore());
        assertEquals("mobicents/aap/1", nameGenerator.next());

        assertTrue("Two names remains", nameGenerator.hasMore());
        assertEquals("mobicents/aap/2", nameGenerator.next());

        assertTrue("One name remains", nameGenerator.hasMore());
        assertEquals("mobicents/aap/3", nameGenerator.next());

        assertFalse("No more names remains", nameGenerator.hasMore());
    }

    @Test
    public void testSinglePattern2() {
        String pattern = "mobicents/[1..3]/aap";
        nameGenerator.setPattern(pattern);

        assertTrue("Three names remains", nameGenerator.hasMore());
        assertEquals("mobicents/1/aap", nameGenerator.next());

        assertTrue("Two names remains", nameGenerator.hasMore());
        assertEquals("mobicents/2/aap", nameGenerator.next());

        assertTrue("One name remains", nameGenerator.hasMore());
        assertEquals("mobicents/3/aap", nameGenerator.next());

        assertFalse("No more names remains", nameGenerator.hasMore());
    }

    @Test
    public void testSinglePattern3() {
        String pattern = "[1..3]/mobicents/aap";
        nameGenerator.setPattern(pattern);

        assertTrue("Three names remains", nameGenerator.hasMore());
        assertEquals("1/mobicents/aap", nameGenerator.next());

        assertTrue("Two names remains", nameGenerator.hasMore());
        assertEquals("2/mobicents/aap", nameGenerator.next());

        assertTrue("One name remains", nameGenerator.hasMore());
        assertEquals("3/mobicents/aap", nameGenerator.next());

        assertFalse("No more names remains", nameGenerator.hasMore());
    }

    //@Test
    public void testMultiplePatterns() {
        String pattern = "[1..3]/[4..5]/aap";
        nameGenerator.setPattern(pattern);

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("1/4/aap", nameGenerator.next());

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("1/5/aap", nameGenerator.next());

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("2/4/aap", nameGenerator.next());

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("2/5/aap", nameGenerator.next());

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("3/4/aap", nameGenerator.next());

        assertTrue("More remains", nameGenerator.hasMore());
        assertEquals("3/5/aap", nameGenerator.next());

        assertFalse("No more names remains", nameGenerator.hasMore());
    }

}