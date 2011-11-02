/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.fsm;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * @author kulikov
 */
public class FSM implements Runnable, Serializable {
    
    //first and last states in fsm
    protected State start;
    protected State end;
    
    //intermediate states
    private HashMap<String, State> states = new HashMap();
    
    protected State state;
    private ReentrantLock lock = new ReentrantLock();
    
    private HashMap attributes = new HashMap();
    
    //state timer
    protected ScheduledExecutorService scheduler;    
    protected ScheduledFuture timer;
    
    protected Logger logger;
    
    public FSM(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }
    
    public void setLogger(Logger logger) {
        this.logger = logger;
    }
    
    public State getState() {
        return state;
    }
    
    public void setStart(String name) {
        //the start state already has value wich differs from current state? 
        if (this.start != null && state != null) {
            throw new IllegalStateException("Start state can be changed now");
        }
        this.start = states.get(name);
        this.state = start;
    }
    
    public void setEnd(String name) {
        this.end = states.get(name);
    }
    
    public State createState(String name) {
        State s = new State(this, name);
        states.put(name, s);
        return s;
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }
    
    public Object getAttribute(String name) {
        return attributes.get(name);
    }
    
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
    
    public Transition createTransition(String name, String from, String to) {
        if (name.equals("timeout")) {
            throw new IllegalArgumentException("timeout is illegal name for transition");
        }
        
        if (!states.containsKey(from)) {
            throw new IllegalStateException("Unknown state: " + from);
        }
        
        if (!states.containsKey(to)) {
            throw new IllegalStateException("Unknown state: " + to);
        }
        
        Transition t = new Transition(name,states.get(to));
        states.get(from).add(t);
        
        return t;
    }

    public Transition createTimeoutTransition(String from, String to, long timeout) {
        if (!states.containsKey(from)) {
            throw new IllegalStateException("Unknown state: " + from);
        }
        
        if (!states.containsKey(to)) {
            throw new IllegalStateException("Unknown state: " + to);
        }
        
        Transition t = new Transition("timeout", states.get(to));
        states.get(from).timeout = timeout;
        states.get(from).add(t);
        
        return t;
    }
    
    /**
     * Processes transition.
     * 
     * @param name the name of transition.
     */
    public synchronized void signal(String name) throws UnknownTransitionException {
        //check that start state defined
        if (start == null) {
            throw new IllegalStateException("The start sate is not defined");
        }

        //check that end state defined
        if (end == null) {
            throw new IllegalStateException("The end sate is not defined");
        }

        //ignore any signals if fsm reaches end state
//        if (state == end) {
//            return;
//        }

        String old = state.getName();
        //switch to next state
        state = state.signal(name);
        if (logger != null) {
            logger.debug(String.format("current state=%s, signal=%s, transition to=%s", old, name, state.getName()));
        }
    }
    
    public void signalAsync(String name) {
        new Thread(new Executor(name)).start();
    }
    
    public void run() {
        lock.lock();
        synchronized(this) {
            if (state != null && state != start && state != end) {
                state.tick(System.currentTimeMillis());
            }
        } 
    }
    
    private class Executor implements Runnable {
        private String signal;
        
        public Executor(String signal) {
            this.signal = signal;
        }
        
        public void run() {
            try {
                signal(this.signal);
            } catch (Exception e) {
            }
        }
    }
}
