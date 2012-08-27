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
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import java.util.ArrayList;

import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.hardware.dahdi.Channel;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Transmitter implementation.
 *
 */
public class SS7Output extends AbstractSink {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private AudioFormat destinationFormat;
	
    private Channel channel;
    
    //active formats
    private Formats formats;
    
    private Sender sender;
    
    //signaling processor
    private Processor dsp;                               
        
    //The underlying buffer size
    private static final int QUEUE_SIZE = 10;
    //the underlying buffer
    private ArrayList<Frame> queue = new ArrayList(QUEUE_SIZE);
    
    //number of bytes to send in single cycle
    private static final int SEND_SIZE=32;
    
    /**
     * Creates new transmitter
     */
    protected SS7Output(Scheduler scheduler,Channel channel,AudioFormat destinationFormat) {
        super("Output");
        this.channel=channel;        
        this.destinationFormat=destinationFormat;
        this.sender=new Sender(scheduler);
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

    public void start() {    	
    	super.start();
    	sender.submit();    	    	
    }
    
    public void stop() {
    	super.stop();
    	sender.cancel();
    }
    
    /**
     * (Non Java-doc.)
     *
     *
     * @see org.mobicents.media.MediaSink#setFormats(org.mobicents.media.server.spi.format.Formats)
     */
    public void setFormats(Formats formats) throws FormatNotSupportedException {
    		
    }       

    public void setDestinationFormat(AudioFormat destinationFormat)
    {
    	this.destinationFormat=destinationFormat;    	
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	//do transcoding
    	if (dsp != null && destinationFormat!=null) {
    		try
    		{
    			frame = dsp.process(frame,format,destinationFormat);            			
    		}
    		catch(Exception e)
    		{
    			//transcoding error , print error and try to move to next frame
    			System.out.println(e.getMessage());
    			e.printStackTrace();
    			return;
    		} 
    	}
    	
    	queue.add(frame);    	    	  
    }
    
    private class Sender extends Task {
    	private Frame currFrame=null;
    	private byte[] smallBuffer=new byte[SEND_SIZE];
    	int framePosition=0;
    	int readCount=0;
    	
        public Sender(Scheduler scheduler) {
            super(scheduler);
        }        

        public int getQueueNumber()
        {
        	return scheduler.SENDER_QUEUE;
        }   
        
        public void submit()
        {
        	this.activate(false);
        	scheduler.submit(this,scheduler.SENDER_QUEUE);
        }
        
        @Override
        public long perform() {
        	if(currFrame==null && queue.size()>0)
        	{
        		currFrame=queue.remove(0);
        		framePosition=0;
        	}
        	
        	readCount=0;
        	if(currFrame!=null)
        	{
        		byte[] data=currFrame.getData(); 
        		if(framePosition+SEND_SIZE<data.length)
        		{
        			System.arraycopy(data, framePosition, smallBuffer, 0, 32);
        			readCount=SEND_SIZE;
        			framePosition+=SEND_SIZE;        			
        		}
        		else if(framePosition<data.length-1)
        		{
        			System.arraycopy(data, framePosition, smallBuffer, 0, data.length-framePosition);
        			readCount=data.length-framePosition;
        			currFrame.recycle();
        			currFrame=null;
        			framePosition=0;
        		}    		
        	}
        	
            while(readCount<smallBuffer.length)
            	smallBuffer[readCount++]=(byte)0;
            
            try
            {
            	channel.write(smallBuffer,readCount);            	            
            }
            catch(IOException e)
            {            	            
            }
            
            scheduler.submit(this,scheduler.SENDER_QUEUE);
            return 0;
        }                
    }
}
