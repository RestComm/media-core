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
import java.text.Format;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.hardware.dahdi.SelectorKeyImpl;
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
public class SS7DataChannel {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
	private AudioFormat g711a = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);
	private AudioFormat g711u = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
	
    private final long ssrc = System.currentTimeMillis();

    //SS7 channels
    private Channel channel;
    
    private int channelID;
    
    //Receiver and transmitter
    private SS7Input input;
    private SS7Output output;

    private SS7Handler ss7Handler;

    private SS7Manager ss7Manager;
    
    private volatile long rxCount;
    private volatile long txCount;
    
    private Formats formats = new Formats();
    
    private Logger logger = Logger.getLogger(SS7DataChannel.class) ;
    
    private boolean isALaw=false;
    private boolean shouldLoop=false;
    
    private AudioComponent audioComponent;
    private OOBComponent oobComponent;
    
    /**
     * Create SS7 channel instance.
     *
     * @param SS7 manager , Dahdi Channel ID , Audio Format used on channel
     * @throws IOException
     */
    public SS7DataChannel(SS7Manager ss7Manager,int dahdiChannelID,int audioChannelID,boolean isALaw) throws IOException {
    	this.ss7Manager=ss7Manager;
    	this.ss7Handler=new SS7Handler();
    	this.channelID=dahdiChannelID;
    	this.channel=ss7Manager.open(channelID);
    	this.isALaw=isALaw;
    	
    	if(isALaw)
    	{
    		//receiver
    		input = new SS7Input(ss7Manager.scheduler,channel,g711a);        
    		//transmittor
    		output = new SS7Output(ss7Manager.scheduler,channel,g711a);
    	}
    	else
    	{
    		//receiver
    		input = new SS7Input(ss7Manager.scheduler,channel,g711u);        
    		//transmittor
    		output = new SS7Output(ss7Manager.scheduler,channel,g711u);
    	}
    	
    	audioComponent=new AudioComponent(audioChannelID); 
        audioComponent.addInput(input.getAudioInput());
        audioComponent.addOutput(output.getAudioOutput());
        
        oobComponent=new OOBComponent(audioChannelID);
        oobComponent.addOutput(output.getOOBOutput());
    }
    
    public AudioComponent getAudioComponent()
    {
    	return this.audioComponent;
    }
    
    public OOBComponent getOOBComponent()
    {
    	return this.oobComponent;
    }
    
    public void setOutputDsp(Processor dsp) {
    	output.setDsp(dsp);
    }
    
    public void setInputDsp(Processor dsp) {
    	input.setDsp(dsp);
    }
    
    public void setCodec(boolean isALaw)
    {
    	this.isALaw=isALaw;
    	channel.setCodec(isALaw);
    	if(isALaw)
    	{
    		input.setSourceFormat(g711a);
    		output.setDestinationFormat(g711a);
    	}
    	else
    	{
    		input.setSourceFormat(g711u);
    		output.setDestinationFormat(g711u);
    	}
    }
    
    /**
     * Binds channel to the first available port.
     *
     * @throws SocketException
     */
    public void bind() {      		
    	//bind data channel
    	ss7Manager.bind(channel,ss7Handler);
    	channel.setCodec(isALaw);    		
        input.activate();
    	output.activate();
    	audioComponent.updateMode(true,true);
    	oobComponent.updateMode(true,true);
    }

    /**
     * Gets the port number to which this channel is bound.
     *
     * @return the port number.
     */
    public int getChannelID() {
        return channelID;
    }    

    public void activateLoop() {
    	input.deactivate();
        output.deactivate(); 
        audioComponent.updateMode(false,false);
        oobComponent.updateMode(false,false);
        shouldLoop=true;
    }
    
    public void deactivateLoop() {
    	input.activate();
        output.activate(); 
        audioComponent.updateMode(true,true);
        oobComponent.updateMode(true,true);
        shouldLoop=false;
    }
    
    public boolean inLoop()
    {
    	return shouldLoop;
    }
    
    /**
     * Closes this socket.
     */
    public void close() {
        ss7Manager.unbind(channel);
        	
        rxCount=0;
        txCount=0;
        input.deactivate();
        output.deactivate();  
        audioComponent.updateMode(false,false);
        oobComponent.updateMode(false,false);
    }    

    public int getPacketsLost() {
        return input.getPacketsLost();
    }

    public long getPacketsReceived() {
        return rxCount;
    }

    public long getPacketsTransmitted() {
        return txCount;
    }             
    
    /**
     * Implements IO operations for RTP protocol.
     *
     * This class is attached to channel and when channel is ready for IO
     * the scheduler will call either receive or send.
     */
    private class SS7Handler implements ProtocolHandler {
        //The schedulable task for read operation
        private volatile boolean isReading = false;
        private volatile boolean isWritting;
        private int readBytes=0;
        private byte[] smallBuffer=new byte[32];
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#receive(java.nio.channels.DatagramChannel)
         */
        public void receive(Channel channel) {
        		if(!shouldLoop)
        			input.readData();
        		else
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
        	    	
        	    	while(readBytes<smallBuffer.length)
                    	smallBuffer[readBytes++]=(byte)0;
                    
                    try
                    {
                    	channel.write(smallBuffer,readBytes);            	            
                    }
                    catch(IOException e)
                    {            	            
                    }
        		}
        }

        public boolean isReadable() {
            return !this.isReading;
        }

        public boolean isWriteable() {
            return true;
        }

        protected void allowReading() {
                this.isReading = false;            
        }        
        
        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#send(java.nio.channels.DatagramChannel)
         */
        public void send(Channel channel) {        		        
        }

        public void setKey(SelectorKeyImpl key) {
        }
    }    
}
