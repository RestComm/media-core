/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.controller;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.control.mgcp.controller.Matcher;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class MatcherTest {
    
    private Matcher matcher = new Matcher();
    
    public MatcherTest() {
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
     * Test of match method, of class Matcher.
     */
    @Test
    public void testMatch() {
        assertTrue("Must match", matcher.match("mobicents/ivr/1", "mobicents/ivr/$"));
    }
}
