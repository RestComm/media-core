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

package org.restcomm.media.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduling task.
 * 
 * @author Oifa Yulian
 */
public abstract class Task implements Runnable {
	private static AtomicInteger id=new AtomicInteger(0);
	
    private volatile boolean isActive = true;
    private volatile boolean isHeartbeat = true;
    //error handler instance
    protected TaskListener listener;
    
    private final Object LOCK = new Object();    
        
    private AtomicBoolean inQueue0=new AtomicBoolean(false);
    private AtomicBoolean inQueue1=new AtomicBoolean(false);
    
    private Logger logger = LogManager.getLogger(Task.class);
    
    protected int taskId;
    
    public Task() {
    	taskId=id.incrementAndGet();
    }

    public void storedInQueue0()
    {
    	inQueue0.set(true);
    }
    
    public void storedInQueue1()
    {
    	inQueue1.set(true);
    }
    
    public void removeFromQueue0()
    {
    	inQueue0.set(false);
    }
    
    public void removeFromQueue1()
    {
    	inQueue1.set(false);
    }
   
    public Boolean isInQueue0()
    {
    	return inQueue0.get();
    }
    
    public Boolean isInQueue1()
    {
    	return inQueue1.get();
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
    	}
    }

    //call should not be synchronized since can run only once in queue cycle
    public void run() {
    		if (this.isActive)  {
    			try {
    				perform();                
                
    				//notify listener                
    				if (this.listener != null) {
    					this.listener.onTerminate();
    				}
    				
    			} catch (Exception e) {
    			    logger.error("Could not execute task " + this.taskId + ": "+ e.getMessage(), e);
    				if (this.listener != null) {
    					listener.handlerError(e);
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
