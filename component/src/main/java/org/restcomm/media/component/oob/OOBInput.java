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

package org.restcomm.media.component.oob;

import java.io.IOException;

import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.concurrent.ConcurrentCyclicFIFO;
import org.restcomm.media.spi.memory.Frame;

/**
 * Implements input for compound components
 * 
 * @author Yulian Oifa
 */
public class OOBInput extends AbstractSink {
	
	private static final long serialVersionUID = -5568937038806140983L;

	private int inputId;
    private int limit=10;
    private ConcurrentCyclicFIFO<Frame> buffer = new ConcurrentCyclicFIFO<Frame>();
    
    /**
     * Creates new stream
     */
    public OOBInput(int inputId) {
        super("compound.input");
        this.inputId=inputId;        
    }
    
    public int getInputId() {
    	return inputId;
    }
    
    @Override
    public void activate() {
    }
    
    @Override
    public void deactivate() {
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	if (buffer.size() >= limit) {
    		buffer.poll().recycle();
    	} 
    	buffer.offer(frame);    	
    }

    /**
     * Indicates the state of the input buffer.
     *
     * @return true if input buffer has no frames.
     */
    public boolean isEmpty() {
        return buffer.size() == 0;
    }

    /**
     * Retrieves frame from the input buffer.
     *
     * @return the media frame.
     */
    public Frame poll() {
    	return buffer.poll();
    }

    /**
     * Recycles input stream
     */
    public void recycle() {
    	while(buffer.size()>0) {
    		buffer.poll().recycle();    	    			       
    	}
    }
    
    public void resetBuffer() {
    	while(buffer.size()>0) {
    		buffer.poll().recycle();
    	}
    }
}
