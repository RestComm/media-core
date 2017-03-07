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

package org.restcomm.fsm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.fsm.FSM;
import org.restcomm.fsm.State;
import org.restcomm.fsm.StateEventHandler;
import org.restcomm.fsm.TransitionHandler;
import org.restcomm.fsm.UnknownTransitionException;

import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class MultipleFSMTest {

    
    protected final static String STATE_NULL = "NULL";
    protected final static String STATE_SOFT_CONNECTING = "SOFT_CONNECTING";
    protected final static String STATE_CONNECTING = "CONNECTING";
    protected final static String STATE_CONNECTED = "CONNECTED";
    protected final static String STATE_DISCONNECTING = "DISCONNECTING";
    protected final static String STATE_INVALID = "INVALID";
    protected final static String STATE_CANCELED = "CANCELED";
    
    protected final static String SIGNAL_JOIN = "join";
    protected final static String SIGNAL_SOFT_JOIN = "soft_joined";
    protected final static String SIGNAL_JOINED = "joined";
    protected final static String SIGNAL_OTHER_PARTY_JOINED = "other_party_joined";
    protected final static String SIGNAL_UNJOIN = "unjoin";
    protected final static String SIGNAL_UNJOINED = "unjoined";
    protected final static String SIGNAL = "other_party_unjoined";
    protected final static String SIGNAL_FAILURE = "failure";

    
    private FSM fsm1,fsm2;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private boolean onEnterEvent = false;
    private boolean onExit = false;
    private boolean transition = false;
    
    public MultipleFSMTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        fsm1 = this.createFSM();
        fsm2 = this.createFSM();
    }

    @After
    public void tearDown() {
        scheduler.shutdownNow();
    }


    private FSM createFSM() {
        FSM fsm = new FSM(scheduler);
        
        //define states
        fsm.createState(STATE_NULL);
        fsm.createState(STATE_CONNECTING).setOnEnter(new OnConnecting());
        
        //on enter triggers other party with signal soft_join
        fsm.createState(STATE_SOFT_CONNECTING);
        
        //on enter triggers other party with signal joined
        fsm.createState(STATE_CONNECTED).setOnEnter(new OnConnected());
        
        //on enter triggers delete connection request
        fsm.createState(STATE_DISCONNECTING).setOnEnter(new UnjoinRequest(fsm));
        
        //on enter triggers unjoin on other party and cleans this connection
        //sends unjoin event
        fsm.createState(STATE_INVALID).setOnEnter(new OnDisconnected());
        
        fsm.createState(STATE_CANCELED);
        
        fsm.setStart(STATE_NULL);
        fsm.setEnd(STATE_INVALID);
        
        //define transitions
        
        /*-----------------------------------------------------------------*/
        /*             STATE NULL                                          */
        /*-----------------------------------------------------------------*/
        //initiate joining (first connection)
        fsm.createTransition(SIGNAL_JOIN, STATE_NULL, STATE_CONNECTING).setHandler(
                new JoinRequest(fsm));
        //jump to connecting state caused by master connectin
        fsm.createTransition(SIGNAL_SOFT_JOIN, STATE_NULL, STATE_SOFT_CONNECTING);
        
        /*-----------------------------------------------------------------*/
        /*             STATE CONNECTING                                    */
        /*-----------------------------------------------------------------*/
        //positive response from server
        fsm.createTransition(SIGNAL_JOINED, STATE_CONNECTING, STATE_CONNECTED);
        //unjoin called by user in the middle of joining process
        fsm.createTransition(SIGNAL_UNJOIN, STATE_CONNECTING, STATE_CANCELED);
        //negative response from server
        fsm.createTransition(SIGNAL_FAILURE, STATE_CONNECTING, STATE_INVALID);
        //no response from server
        fsm.createTimeoutTransition(STATE_CONNECTING, STATE_INVALID, 5000);

        /*-----------------------------------------------------------------*/
        /*             STATE SOFT_CONNECTING                                    */
        /*-----------------------------------------------------------------*/
        //positive response from server
        fsm.createTransition(SIGNAL_JOINED, STATE_SOFT_CONNECTING, STATE_CONNECTED);
        //unjoin called by user in the middle of joining process
        fsm.createTransition(SIGNAL_UNJOIN, STATE_SOFT_CONNECTING, STATE_INVALID);
        
        /*-----------------------------------------------------------------*/
        /*             STATE CONNECTED                                     */
        /*-----------------------------------------------------------------*/
        //request went to drop connection on server side
        fsm.createTransition(SIGNAL_UNJOIN, STATE_CONNECTED, STATE_DISCONNECTING);
        
        /*-----------------------------------------------------------------*/
        /*             STATE DISCONNECTING                                 */
        /*-----------------------------------------------------------------*/
        //positive response from server
        fsm.createTransition(SIGNAL_UNJOINED, STATE_DISCONNECTING, STATE_INVALID);
        //no response from server
        fsm.createTimeoutTransition(STATE_DISCONNECTING, STATE_INVALID, 5000);
        //TODO negative response with next try to send request
        
        /*-----------------------------------------------------------------*/
        /*             STATE CANCELED                                      */
        /*-----------------------------------------------------------------*/
        //positve response but connection already canceled by user
        fsm.createTransition(SIGNAL_JOIN, STATE_CANCELED, STATE_DISCONNECTING);
        //negative response but connection was already canceled by user
        fsm.createTransition(SIGNAL_FAILURE, STATE_CANCELED, STATE_INVALID);
        //no response from server
        fsm.createTimeoutTransition(STATE_CANCELED, STATE_INVALID, 5000);
        return fsm;
    }
    /**
     * Test of setStart method, of class FSM.
     */
    @Test
    public void testTransitions() throws UnknownTransitionException, InterruptedException {
        fsm1.signal(SIGNAL_JOIN);

        Thread.sleep(1000);
        
        assertEquals(STATE_CONNECTING, fsm1.getState().getName());
        assertEquals(STATE_SOFT_CONNECTING, fsm2.getState().getName());
        
        Thread.sleep(4000);
        
        assertEquals(STATE_CONNECTED, fsm1.getState().getName());
        assertEquals(STATE_CONNECTED, fsm2.getState().getName());
        
        fsm1.signal(SIGNAL_UNJOIN);

        Thread.sleep(3000);
        
        assertEquals(STATE_INVALID, fsm1.getState().getName());
        assertEquals(STATE_INVALID, fsm2.getState().getName());
    }

    private FSM otherParty(State state) {
        if (state.getFSM() == fsm1) {
            return fsm2;
        }
        return fsm1;
    }
    
    private class OnConnecting implements StateEventHandler {

        public void onEvent(State state) {
            try {
                otherParty(state).signal(SIGNAL_SOFT_JOIN);
            } catch (UnknownTransitionException e) {
                e.printStackTrace();
            }
        }
        
    }    

    private class OnConnected implements StateEventHandler, Runnable  {
        
        private State state;
        
        public void onEvent(State state) {
            this.state = state;
            new Thread(this).start();
        }
        
        public void run() {
            try {
                    otherParty(state).signal(SIGNAL_JOINED);
            } catch (UnknownTransitionException e) {
            }
        }
    }    
    
    private class JoinRequest implements TransitionHandler, Runnable {

        private FSM fsm;

        private JoinRequest(FSM fsm) {
            this.fsm = fsm;
        }

        public void process(State state) {
            new Thread(this).start();
        }

        public void run() {
            try {
                Thread.sleep(3000);
                fsm.signal(SIGNAL_JOINED);
            } catch (UnknownTransitionException e) {
            } catch (InterruptedException e) {
            }
        }
    }

    private class UnjoinRequest implements StateEventHandler, Runnable {

        private FSM fsm;

        public UnjoinRequest(FSM fsm) {
            this.fsm = fsm;
        }

        public void onEvent(State state) {
            new Thread(this).start();
        }

        public void run() {
            try {
                fsm.signal(SIGNAL_UNJOINED);
            } catch (UnknownTransitionException e) {
            }
        }
    }
    
    private class OnDisconnected implements StateEventHandler, Runnable {

        private State state;

        public OnDisconnected() {
        }

        public void onEvent(State state) {
            this.state = state;
            new Thread(this).start();
        }

        public void run() {
            try {
                otherParty(state).signal(SIGNAL_UNJOIN);
            } catch (UnknownTransitionException e) {
                e.printStackTrace();
            }
        }
    }    
}