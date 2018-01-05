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

package org.restcomm.media.component;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.MediaSink;
import org.restcomm.media.spi.memory.Frame;
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

	private static final long serialVersionUID = -2119158462149998609L;

	//shows if component is started or not.
    private volatile boolean started = false;
    
    //transmission statisctics
    private volatile long rxPackets;
    private volatile long rxBytes;    
    
    private static final Logger logger = LogManager.getLogger(AbstractSink.class);
    
    /**
     * Creates new instance of sink with specified name.
     * 
     * @param name the name of the sink to be created.
     */
    public AbstractSink(String name) {
        super(name);               
    }        

    @Override
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
    protected void start() {
    	if (started) {
			return;
		}

		//change state flag
		started = true;
		
		this.rxBytes = 0;
		this.rxPackets = 0;

		//send notification to component's listener
		started();		    	
    }    
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#stop().
     */
    protected void stop() {
    	started = false;
		stopped();    	
    }

    @Override
    public abstract void activate();
    
    @Override
    public abstract void deactivate();
    
    /**
     * Sends failure notification.
     * 
     * @param eventID failure event identifier.
     * @param e the exception caused failure.
     */
    protected void failed(Exception e) {
    }

    @Override
    public long getPacketsReceived() {
        return rxPackets;
    }

    @Override
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
     */
    protected void stopped() {
    }    

    public String report() {
    	return "";
    }
    
    @Override
    public void perform(Frame frame) {
    	if(!started) {
    		return;
    	}
    	
    	if(frame==null) {
    		return;
    	}
    	
    	rxPackets++;
    	rxBytes += frame.getLength();

    	//frame is not null, let's handle it
    	try {
    		onMediaTransfer(frame);
    	} catch (IOException e) {  
    		logger.error(e);
    		started = false;
        	failed(e);
    	}
    }    
}
