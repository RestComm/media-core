/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.restcomm.fsm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.fsm.FSM;
import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;
import org.restcomm.fsm.Transition;
import org.restcomm.fsm.TransitionHandler;
import org.restcomm.fsm.UnknownTransitionException;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class FSMTest {

    private FSM fsm;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private boolean onEnterEvent = false;
    private boolean onExit = false;
    private boolean transition = false;
    
    public FSMTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        fsm = new FSM(scheduler);
    }

    @After
    public void tearDown() {
        scheduler.shutdownNow();
    }

    /**
     * Test of setStart method, of class FSM.
     */
    @Test
    public void testSetStart() {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.setStart("start");
        fsm.setEnd("end");
        
        assertEquals("start", fsm.getState().getName());
    }

    @Test
    public void testSetStart1() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.setStart("start");
        fsm.setEnd("end");

        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");
        
        fsm.signal("t1");

        try {
            fsm.setStart("start");
            fail("State has changed");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testCreateTransitions() {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.setStart("start");
        fsm.setEnd("end");

        try {
            fsm.createTransition("t1", "start", "state1");
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
    

    @Test
    public void testIllegalTransitions() {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.setStart("start");
        fsm.setEnd("end");

        try {
            fsm.createTransition("t1", "start", "state2");
            fail("State2 is unknown transition");
        } catch (Exception e) {
        }
    }
    
    
    /**
     * Test of setStart method, of class FSM.
     */
    @Test
    public void testTransition() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");

        fsm.setStart("start");
        fsm.setEnd("end");
        
        assertEquals("start", fsm.getState().getName());
        
        fsm.signal("t1");
        assertEquals("state1", fsm.getState().getName());
        
        fsm.signal("t2");
        assertEquals("end", fsm.getState().getName());
    }

    /**
     * Test of setStart method, of class FSM.
     */
    @Test
    public void testUnknownTransition() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");

        fsm.setStart("start");
        fsm.setEnd("end");

        try {
            fsm.signal("t6");
            fail("Unknown transition");
        } catch (UnknownTransitionException e) {
        }
    }

    @Test
    @SuppressWarnings("static-access")
    public void testTimeoutTransition() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");
        fsm.createTimeoutTransition("state1", "end", 3000);
        
        fsm.setStart("start");
        fsm.setEnd("end");

//        scheduler.scheduleAtFixedRate(fsm, 0, 1, TimeUnit.SECONDS);
        fsm.signal("t1");
        
        try {
            Thread.currentThread().sleep(5000);
        } catch (Exception e) {
            fail("Interrupted");
        }
        
        assertEquals("end", fsm.getState().getName());
    }
    

    @Test
    public void testStartNotSet() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");

        try {
            fsm.signal("t1");
            fail("Start state is not set");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testEndNotSet() throws UnknownTransitionException {
        fsm.createState("start");
        fsm.createState("state1");
        fsm.createState("end");
        
        fsm.createTransition("t1", "start", "state1");
        fsm.createTransition("t2", "state1", "end");

        fsm.setStart("start");
        
        try {
            fsm.signal("t1");
            fail("End state is not set");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testAttributes() throws UnknownTransitionException {
        fsm.setAttribute("a", "hello");
        assertEquals("hello", fsm.getAttribute("a"));
        
        fsm.removeAttribute("a");
        assertEquals(null, fsm.getAttribute("a"));
    }

    @Test
    public void testHandlers() throws UnknownTransitionException, InterruptedException {
        State start = fsm.createState("start");
        start.setOnExit(new OnExit());
        
        State state1 = fsm.createState("state1");
        state1.setOnEnter(new OnEnter());
        
        fsm.createState("end");
        
        Transition t = fsm.createTransition("t1", "start", "state1");
        t.setHandler(new OnTransition());
        
        fsm.createTransition("t2", "state1", "end");

        fsm.setStart("start");
        fsm.setEnd("end");
        
        fsm.signal("t1");
        
        Thread.sleep(1000);
        
        assertEquals(true, onEnterEvent);
        assertEquals(true, onExit);
        assertEquals(true, transition);
    }
    
    private class OnEnter implements StateEventHandler {

        public void onEvent(State state) {
            onEnterEvent = true;
        }
        
    }
    
    private class OnExit implements StateEventHandler {

        public void onEvent(State state) {
            onExit = true;
        }
        
    }
    
    private class OnTransition implements TransitionHandler {

        public void process(State state) {
            transition = true;
        }
        
    }
    
}
