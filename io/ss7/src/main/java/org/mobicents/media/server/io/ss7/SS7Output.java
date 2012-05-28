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
        
    private byte[] buffer=new byte[800];
    private int readIndex=0;
    private int writeIndex=0;
    
    /**
     * Creates new transmitter
     */
    protected SS7Output(Scheduler scheduler,Channel channel,AudioFormat destionationFormat) {
        super("Output", scheduler,scheduler.OUTPUT_QUEUE);
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
    	super.stop();
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

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
    	//do transcoding
    	if (dsp != null && formats!=null && !formats.isEmpty()) {
    		try
    		{
    			frame = dsp.process(frame,format,formats.get(0));            			
    		}
    		catch(Exception e)
    		{
    			//transcoding error , print error and try to move to next frame
    			System.out.println(e.getMessage());
    			e.printStackTrace();
    			return;
    		} 
    	}
    	    	
    	byte[] data=frame.getData();
    	for(int i=0;i<data.length;i++)
    	{
    		buffer[writeIndex]=data[i];
    		writeIndex=(writeIndex+1)%buffer.length;
    	}    	    	
    }
    
    private class Sender extends Task {
    	byte[] smallBuffer=new byte[32];
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
        	readCount=0;
            while(readIndex!=writeIndex && readCount<smallBuffer.length)
            {
            	smallBuffer[readCount++]=buffer[readIndex];
            	readIndex=(readIndex+1)%buffer.length;
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
