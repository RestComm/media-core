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

package org.mobicents.media.core.endpoints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.server.component.audio.AudioSplitter;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointState;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Basic implementation of the endpoint.
 * 
 * @author yulian oifa
 * @author amit bhayani
 */
public class BaseSplitterEndpointImpl extends BaseEndpointImpl {
	
	protected AudioSplitter audioSplitter;
	
	private AtomicInteger loopbackCount=new AtomicInteger(0);
	private AtomicInteger readCount=new AtomicInteger(0);
	private AtomicInteger writeCount=new AtomicInteger(0);
	
	public BaseSplitterEndpointImpl(String localName) {
        super(localName);              
    }        
    
    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#start()
     */
    public void start() throws ResourceUnavailableException {
    	super.start();
    	
    	audioSplitter=new AudioSplitter(getScheduler());    	       
    }    
    
    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.server.spi.Endpoint#createConnection(org.mobicents.media.server.spi.ConnectionMode);
     */
    public Connection createConnection(ConnectionType type,Boolean isLocal) throws ResourceUnavailableException {
    	Connection connection=super.createConnection(type,isLocal);
    	
    	switch(type)
    	{
    		case RTP:
    			audioSplitter.addOutsideComponent(((BaseConnection)connection).getAudioComponent());        
    			break;
    		case LOCAL:
    			audioSplitter.addInsideComponent(((BaseConnection)connection).getAudioComponent());        
    	    	break;
    	}
    	
    	return connection;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.spi.Endpoint#deleteConnection(Connection)
     */
    public void deleteConnection(Connection connection,ConnectionType connectionType) {
    	super.deleteConnection(connection,connectionType);
    	
    	switch(connectionType)
    	{
    		case RTP:
    			audioSplitter.releaseOutsideComponent(((BaseConnection)connection).getAudioComponent());        
    			break;
    		case LOCAL:
    			audioSplitter.releaseInsideComponent(((BaseConnection)connection).getAudioComponent());        
    	    	break;
    	}    	
    }
    
    //should be handled on higher layers
    public void modeUpdated(ConnectionMode oldMode,ConnectionMode newMode)
    {
    	int readCount=0,loopbackCount=0,writeCount=0;
    	switch(oldMode)
    	{
    		case RECV_ONLY:
    			readCount-=1;
    			break;
    		case SEND_ONLY:
    			writeCount-=1;
    			break;
    		case SEND_RECV:
    		case CONFERENCE:
    			readCount-=1;
    			writeCount-=1;    			
    			break;
    		case NETWORK_LOOPBACK:
    			loopbackCount-=1;
    			break;
    	}
    	
    	switch(newMode)
    	{
    		case RECV_ONLY:
    			readCount+=1;
    			break;
    		case SEND_ONLY:
    			writeCount+=1;
    			break;
    		case SEND_RECV:
    		case CONFERENCE:
    			readCount+=1;
    			writeCount+=1;    			
    			break;
    		case NETWORK_LOOPBACK:
    			loopbackCount+=1;
    			break;
    	}
    	    	
    	if(readCount!=0 || writeCount!=0 || loopbackCount!=0)
    	{
    		//something changed
    		loopbackCount=this.loopbackCount.addAndGet(loopbackCount);
    		readCount=this.readCount.addAndGet(readCount);
    		writeCount=this.writeCount.addAndGet(writeCount);
    		
    		if(loopbackCount>0 || readCount==0 || writeCount==0)
    			audioSplitter.stop();
    		else
    			audioSplitter.start();
    	}    		
    }
}
