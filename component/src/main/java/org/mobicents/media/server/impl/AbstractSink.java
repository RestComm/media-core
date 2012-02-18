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

package org.mobicents.media.server.impl;

import java.io.IOException;

import org.mobicents.media.MediaSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.io.Pipe;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * The base implementation of the media sink.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement 
 * general wiring construct. 
 * All media components have to extend one of these classes.
 * 
 * @author Oifa Yulian
 */
public abstract class AbstractSink extends BaseComponent implements MediaSink {

    //shows if component is started or not.
    private volatile boolean started = false;
    //synchronization flag
    private volatile boolean isSynchronized = false;

    //transmission statisctics
    private volatile long rxPackets;
    private volatile long rxBytes;    
    
    //media transmission pipe
    private PipeImpl pipe;    
    
    //scheduler instance
    private final Scheduler scheduler;

    //receiver and transcoding task
    private final Worker worker;        
    
    /**
     * Creates new instance of sink with specified name.
     * 
     * @param name the name of the sink to be created.
     */
    public AbstractSink(String name, Scheduler scheduler,int queueNumber) {
        super(name);
        this.scheduler = scheduler;
        this.worker = new Worker(scheduler,queueNumber);        
    }        

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.MediaSink#connect(org.mobicents.media.server.spi.io.Pipe)
     */
    public void connect(Pipe pipe) {
        this.pipe = (PipeImpl) pipe;
        this.pipe.sink.set(this);
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.MediaSink#disconnect(org.mobicents.media.server.spi.io.Pipe)
     */
    public void disconnect(Pipe pipe) {
        ((PipeImpl)pipe).sink.set(null);
        this.pipe = null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isConnected().
     */
    public boolean isConnected() {
        return pipe != null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isStarted().
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * This methos is called when new portion of media arrives.
     * 
     * @param buffer the new portion of media data.
     */
    public abstract void onMediaTransfer(Frame frame) throws IOException;

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#start().
     */
    public void start() {
    	synchronized(worker) {
    		//silently ignore this call if sink already started
    		if (started) {
    			return;
    		}

    		//change state flag
    		started = true;
    		this.isSynchronized = true;

    		this.rxBytes = 0;
    		this.rxPackets = 0;
        
    		//schedule read task with highest possible priority
    		scheduler.submit(worker,worker.getQueueNumber());

    		//send notification to component's listener
    		started();
    	}
    }

    /**
     * Restores synchronization
     */
    protected void wakeup() {
        synchronized(worker) {
            if (!started) {
                return;
            }

            if (!this.isSynchronized) {
                this.isSynchronized = true;
                scheduler.submit(worker,worker.getQueueNumber());
            }
        }
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#stop().
     */
    public void stop() {
    	synchronized(worker) {
    		started = false;
    		worker.cancel();
    		stopped();
    	}
    }

    /**
     * Sends failure notification.
     * 
     * @param eventID failure event identifier.
     * @param e the exception caused failure.
     */
    protected void failed(Exception e) {
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#getPacketsReceived().
     */
    public long getPacketsReceived() {
        return rxPackets;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#getBytesReceived() 
     */
    public long getBytesReceived() {
        return rxBytes;
    }

    @Override
    public void reset() {
        this.rxPackets = 0;
        this.rxBytes = 0;        
    }

    /**
     * Sends notification that media processing has been started.
     */
    protected void started() {
    }

    /**
     * Sends notification that detection is terminated.
     * 
     */
    protected void stopped() {
    }

    /* (non-Javadoc)
     * @see org.mobicents.media.MediaSink#getInterface(java.lang.Class)
     */
    @Override
    public <T> T getInterface(Class<T> interfaceType) {
        //should we check default?
        return null;
    }

    public String report() {
    	return "";
    }
    
    /**
     * Receiver and transcoding task
     */
    private class Worker extends Task {

        /**
         * Creates new instance of task.
         *
         * @param scheduler scheduler.
         */
    	private int queueNumber;
    	int frameCount=0;  
        long overallDelay=0;
        long frameDuration;
        
        public Worker(Scheduler scheduler,int queueNumber) {
        	super(scheduler);
        	this.queueNumber=queueNumber;            
        }

        public int getQueueNumber()
        {
        	return queueNumber;
        }
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.scheduler.Task#perform()
         */
        public long perform() {
            if (pipe == null) {
                //no source of data
            	isSynchronized = false;
            	return 0;
            }                        
            
            frameCount=0;  
            overallDelay=0;
            while(overallDelay<20000000L)
            {
            	frameCount++;
            	
            	//Reading next frame
            	Frame frame = pipe.read();

            	if (frame == null) {
            		if(frameCount==1)
            		{
            			//los of synchronization
            			isSynchronized = false;
            			return 0;
            		}
            		else
            		{
            			//got all packets but did not lost a sync
            			scheduler.submit(this,queueNumber);             
                        return 0;
            		}
            	}            	
            	
            	//buffer.offer(frame);
            	frameDuration = frame.getDuration();
            	overallDelay+=frameDuration;
            	
            	rxPackets++;
            	rxBytes += frame.getLength();

            	//frame is not null, let's handle it
            	try {
            		onMediaTransfer(frame);
            	} catch (IOException e) {  
            		e.printStackTrace();
            		started = false;
                	failed(e);
            	}
            
            	//check synchronization
            	if (frameDuration == 0 || frameDuration == Long.MAX_VALUE) {
            		//los of synchronization
            		isSynchronized = false;
                	return 0;
            	}                        
            }
            
            scheduler.submit(this,queueNumber);             
            return 0;
        }

        @Override
        public String toString() {
            return getName();
        }
    }    
}
