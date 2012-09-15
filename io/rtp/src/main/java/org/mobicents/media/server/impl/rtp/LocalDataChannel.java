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

package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.component.audio.CompoundInput;
import org.mobicents.media.server.component.audio.CompoundOutput;
import org.mobicents.media.server.component.audio.CompoundComponent;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.AudioFormat;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Local Channel implementation.
 *
 * Bridge between 2 endpoints
 */
public class LocalDataChannel {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;
    
    private CompoundComponent compoundComponent;
	private CompoundInput input;
	private CompoundOutput output;
	
	private LocalDataChannel otherChannel=null;
	
	/**
     * Creates new local channel.
     */
    protected LocalDataChannel(ChannelsManager channelsManager,int channelId) {
    	compoundComponent=new CompoundComponent(channelId); 
    	input=new CompoundInput(1,packetSize);
    	output=new CompoundOutput(channelsManager.getScheduler(),2);
    	compoundComponent.addInput(input);
    	compoundComponent.addOutput(output);
    }

    public CompoundInput getCompoundInput()
    {
    	return this.input;
    }
    
    public CompoundOutput getCompoundOutput()
    {
    	return this.output;
    }
    
    public CompoundComponent getCompoundComponent()
    {
    	return this.compoundComponent;
    }
    
    public void join(LocalDataChannel otherChannel) throws IOException
    {
    	if(this.otherChannel!=null)
    		throw new IOException("Channel already joined");
    	
    	this.otherChannel=otherChannel;
    	otherChannel.otherChannel=this;
    	this.otherChannel.getCompoundOutput().join(input);
    	output.join(this.otherChannel.getCompoundInput());
    }
    
    public void unjoin()
    {
    	if(this.otherChannel==null)
    		return;
    	
    	this.output.deactivate();    	
    	output.unjoin();    	
    	this.otherChannel=null;
    }
    
    public void updateMode(ConnectionMode connectionMode) throws ModeNotSupportedException 
    {
    	if(this.otherChannel==null)
    		 throw new ModeNotSupportedException("You should join channel first");
    	
    	switch (connectionMode) {
        	case SEND_ONLY:
        		compoundComponent.updateMode(false,true);
        		output.activate();        		
        		break;
        	case RECV_ONLY:
        		compoundComponent.updateMode(true,false);
        		output.deactivate();        		
        		break;
        	case INACTIVE:
        		compoundComponent.updateMode(false,false);
        		output.deactivate();        		
        		break;
        	case SEND_RECV:
        	case CONFERENCE:
        		compoundComponent.updateMode(true,true);
        		output.activate();        		
        		break;
        	case NETWORK_LOOPBACK:
        		throw new ModeNotSupportedException("Loopback not supported on local channel");        		
    	}       	
    }    
}