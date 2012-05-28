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
    /**
     * Create SS7 channel instance.
     *
     * @param SS7 manager , Dahdi Channel ID , Audio Format used on channel
     * @throws IOException
     */
    public SS7DataChannel(SS7Manager ss7Manager,int channelID,boolean isALaw) throws IOException {
    	this.ss7Manager=ss7Manager;
    	this.channelID=channelID;    	
    	channel=ss7Manager.open(channelID);
    	
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
    }
    
    /**
     * Binds channel to the first available port.
     *
     * @throws SocketException
     */
    public void bind() {    	
    	//bind data channel
    	ss7Manager.bind(channel,ss7Handler);      
    	input.start();
    	output.start();
    }

    /**
     * Gets the port number to which this channel is bound.
     *
     * @return the port number.
     */
    public int getChannelID() {
        return channelID;
    }    

    /**
     * Closes this socket.
     */
    public void close() {
        ss7Manager.unbind(channel);
        	
        //System.out.println("RX COUNT:" + rxCount + ",TX COUNT:" + txCount);
        rxCount=0;
        txCount=0;
        input.stop();
        output.stop();                       
    }

    /**
     * Gets the receiver stream.
     *
     * @return receiver as media source
     */
    public SS7Input getInput() {
        return input;
    }

    /**
     * Gets the transmitter stream.
     *
     * @return transceiver as media sink.
     */
    public SS7Output getOutput() {
        return output;
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

        /**
         * (Non Java-doc.)
         *
         * @see org.mobicents.media.server.io.network.ProtocolHandler#receive(java.nio.channels.DatagramChannel)
         */
        public void receive(Channel channel) {
        		input.readData();        	
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
