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
package org.restcomm.fsm;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Implements single state of the FSM.
 * 
 * @author kulikov
 */
public class State implements Runnable {
    
    private String name;
    private FSM fsm;
    
    private ArrayList<Transition> transitions = new ArrayList();
    protected long timeout;
    private long activated;
    
    private StateEventHandler enterEventHandler;
    private StateEventHandler exitEventHandler;
        
    protected State(FSM fsm, String name) {
        this.name = name;
        this.fsm = fsm;
        this.timeout = 0;
    }
    
    public void setOnEnter(StateEventHandler handler) {
        this.enterEventHandler = handler;
    }
    
    public void setOnExit(StateEventHandler handler) {
        this.exitEventHandler = handler;
    }
    
    protected void enter() {
        this.activated = System.currentTimeMillis();
        if (this.enterEventHandler != null) {
            this.enterEventHandler.onEvent(this);
//            new Thread(new EnterAction(this)).start();
        }
        
        //if this is end state we have to cancel timer generator
        if (this == fsm.end) {
            fsm.timer.cancel(false);
        }
    }
    
    protected void leave() {
        activated = 0;
        if (this.exitEventHandler != null) {
//            new Thread(new LeaveAction(this)).start();
            this.exitEventHandler.onEvent(this);
        }
        
        //leaving start state? process started and we need to start time marks scheduler
        //for timeout measurement
        if (this == fsm.start) {
            fsm.timer = fsm.scheduler.scheduleAtFixedRate(fsm, 0, 1, TimeUnit.SECONDS);
        }
    }
    
    protected void tick(long now) {
        if (timeout > 0 && activated > 0 && (now - activated) > timeout) {
            try {
                fsm.signal("timeout");
            } catch (UnknownTransitionException e) {
            }
        }
    }
    
    public String getName() {
        return name;
    }
    
    public FSM getFSM() {
        return fsm;
    }
    
    protected void add(Transition t) {
        transitions.add(t);
    }
    
    /**
     * Signals to leave this state over specified transition
     * 
     * @param name the name of the transition.
     */
    public State signal(String name) throws UnknownTransitionException {
        Transition t = find(name);
        if (t != null) {
            return t.process(this);
        }
        throw new UnknownTransitionException(name);
    }
    
    /**
     * Searches transition with specified name.
     * 
     * @param name the name of the transition.
     * @return the transition or null if not found.
     */
    private Transition find(String name) {
        for (Transition t : transitions) {
            if (t.getName().matches(name)) {
                return t;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public void run() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private class EnterAction implements Runnable {
        private State state;
        
        private EnterAction(State state) {
            this.state = state;
        }
        
        public void run() {
            enterEventHandler.onEvent(state);
        }
    }
    
    private class LeaveAction implements Runnable {
        private State state;
        
        private LeaveAction(State state) {
            this.state = state;
        }
        
        public void run() {
            exitEventHandler.onEvent(state);
        }
    }
    
}
