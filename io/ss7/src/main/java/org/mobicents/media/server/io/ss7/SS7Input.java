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

package org.mobicents.media.server.io.ss7;

import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;

import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.hardware.dahdi.Channel;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.io.ss7.ProtocolHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Memory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.apache.log4j.Logger;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Receiver implementation.
 *
 * The Media source of RTP data.
 */
public class SS7Input extends AbstractSource {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private AudioFormat sourceFormat;
	
    //digital signaling processor
    private Processor dsp;
           
    private Channel channel;
    
    private byte[] smallBuffer=new byte[32];
    private byte[] tempBuffer=new byte[160];
    private int currPosition=0;
    private int seqNumber=1;
    private int readBytes=0;
    private int currIndex=0;
    
    private ArrayList<Frame> framesBuffer=new ArrayList(5);
    
    /**
     * Creates new receiver.
     */
    protected SS7Input(Scheduler scheduler,Channel channel,AudioFormat sourceFormat) {
        super("input", scheduler,scheduler.INPUT_QUEUE);               
        this.channel=channel;
        this.sourceFormat=sourceFormat;
    }

    @Override
    public void reset() {
        super.reset();        
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        //assign processor
        this.dsp = dsp;        
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public Processor getDsp() {
        return this.dsp;
    }
    
    protected int getPacketsLost() {
        return 0;
    }    

    @Override
    public Frame evolve(long timestamp) {    	    	    
    	if(framesBuffer.size()>0)
    		return framesBuffer.remove(0);
    	
    	return null;
    }    
    
    public void readData()
    {
    	readBytes=0;    
    	try
    	{
    		readBytes=channel.read(smallBuffer);
    	}
    	catch(IOException e)
    	{
    		
    	}
    	
    	if(readBytes==0)
    		return;
    	
    	currIndex=0;
    	while(currIndex<readBytes && currPosition<tempBuffer.length)
    		tempBuffer[currPosition++]=smallBuffer[currIndex++];
    	
    	if(currPosition==tempBuffer.length)
    	{
    		Frame currFrame=Memory.allocate(tempBuffer.length);
    		//put packet into buffer irrespective of its sequence number
    		currFrame.setHeader(null);
    		currFrame.setSequenceNumber(seqNumber++);
    		//here time is in milliseconds
    		currFrame.setTimestamp(System.currentTimeMillis());
    		currFrame.setOffset(0);
    		currFrame.setLength(tempBuffer.length);
    		System.arraycopy(tempBuffer, 0, currFrame.getData(), 0, tempBuffer.length);

    		//set format
    		currFrame.setFormat(this.sourceFormat);
    		
    		//do the transcoding job
			if (dsp != null) {
				try
				{
					currFrame = dsp.process(currFrame,currFrame.getFormat(),format);
				}
				catch(Exception e)
				{
    				//transcoding error , print error and try to move to next frame
    				System.out.println(e.getMessage());
    				e.printStackTrace();
    			}
    		}   
    		
    		currPosition=0;
    		while(currIndex<readBytes && currPosition<tempBuffer.length)
        		tempBuffer[currPosition++]=smallBuffer[currIndex++];
        	
    		framesBuffer.add(currFrame);
    	}
    }
    
    /**
     * RX buffer's call back method.
     * 
     * This method is called when rxBuffer is full and it is time to start
     * transmission to the consumer.
     */
    public void onFill() {
    	this.wakeup();
    }        
}