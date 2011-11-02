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

package org.mobicents.media.server.scheduler;

/**
 * Scheduling task.
 * 
 * @author kulikov
 */
public abstract class Task {
    protected Scheduler scheduler;

    private volatile boolean isActive = true;
    private volatile boolean isHeartbeat = true;
    //error handler instance
    protected TaskListener listener;
    
    //reference to the chain
    protected TaskChain chain;
    
    private final Object LOCK = new Object();
    
    public Task(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Scheduler scheduler() {
        return this.scheduler;
    }
    
    /**
     * Modifies task listener.
     * 
     * @param listener the handler instance.
     */
    public void setListener(TaskListener listener) {
        this.listener = listener;
    }
    
    /**
     * Current queue of this task.
     * 
     * @return the value of queue
     */
    public abstract int getQueueNumber();    
    
    
    /**
     * Executes task.
     * 
     * @return dead line of next execution
     */
    public abstract long perform();

    /**
     * Cancels task execution
     */
    public void cancel() {
    	synchronized(LOCK) {
    		this.isActive = false;
    		if(isHeartbeat)
    			scheduler.heartBeatQueue.remove(this);
    		else
    			scheduler.taskQueues[getQueueNumber()].remove(this);
    	}
    }

    protected void run() {
    	synchronized(LOCK) {
    		if (this.isActive)  {
    			try {
    				perform();                
                
    				//notify listenet                
    				if (this.listener != null) {
    					this.listener.onTerminate();
    				}
    				//submit next partition
//                	if (chain != null) chain.continueExecution();
    			} catch (Exception e) {
    				if (this.listener != null) listener.handlerError(e);
    			}
    		}
    	}
    }

    protected void activate(Boolean isHeartbeat) {
    	synchronized(LOCK) {
    		this.isActive = true;
    		this.isHeartbeat=isHeartbeat;
    	}
    }
}
