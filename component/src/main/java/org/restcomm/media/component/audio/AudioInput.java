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

package org.restcomm.media.component.audio;

import java.io.IOException;

import org.restcomm.media.component.AbstractSink;
import org.restcomm.media.concurrent.ConcurrentCyclicFIFO;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Implements input for compound components
 * 
 * @author Yulian Oifa
 */
public class AudioInput extends AbstractSink {
	
	private static final long serialVersionUID = -6377790166652701617L;

	private int inputId;
    private int limit=3;
    private ConcurrentCyclicFIFO<Frame> buffer = new ConcurrentCyclicFIFO<Frame>();
    private Frame activeFrame=null;
    private byte[] activeData;
    private byte[] oldData;
    private int byteIndex=0;
    private int count=0;
    private int packetSize=0;
    
    /**
     * Creates new stream
     */
    public AudioInput(int inputId,int packetSize) {
        super("compound.input");
        this.inputId=inputId;
        this.packetSize=packetSize;
    }
    
    public int getInputId()
    {
    	return inputId;
    }
    
    public void activate()
    {
    	
    }
    
    public void deactivate()
    {
    	
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	//generate frames with correct size here , aggregate frames if needed.
    	//allows to accept several sources with different ptime ( packet time ) 
    	oldData=frame.getData();
    	count=0;
    	while(count<oldData.length)
    	{
    		if(activeData==null)
    		{
    			activeFrame=Memory.allocate(packetSize);
    			activeFrame.setOffset(0);
    			activeFrame.setLength(packetSize);
    			activeData=activeFrame.getData(); 
    			byteIndex=0;
    		}
    		
    		if(oldData.length-count<activeData.length-byteIndex)
    		{
    			System.arraycopy(oldData, count, activeData, byteIndex, oldData.length-count);
    			byteIndex+=oldData.length-count;
    			count=oldData.length;    			
    		}
    		else
    		{
    			System.arraycopy(oldData, count, activeData, byteIndex, activeData.length-byteIndex);
    			count+=activeData.length-byteIndex;
    			
    			if (buffer.size() >= limit) 
            		buffer.poll().recycle();
                
            	buffer.offer(activeFrame);
            	
            	activeFrame=null;
    			activeData=null;    			    			
    		}
    	}
    	
    	frame.recycle();
    }

    /**
     * Indicates the state of the input buffer.
     *
     * @return true if input buffer has no frames.
     */
    public boolean isEmpty() {
        return buffer.size()==0;
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
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    	
    	if(activeFrame!=null)
    		activeFrame.recycle();
    	
        activeFrame=null;
		activeData=null;
		byteIndex=0;		        
    }
    
    public void resetBuffer()
    {
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    }
}
