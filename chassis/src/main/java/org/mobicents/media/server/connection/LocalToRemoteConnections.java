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


package org.mobicents.media.server.connection;

import java.util.Enumeration;
import org.mobicents.media.CheckPoint;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.BaseEndpointImpl;
import org.mobicents.media.server.component.Mixer;
import org.mobicents.media.server.component.Splitter;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.video.VideoMixer;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.ResourceUnavailableException;
/**
 * Implements connection management subsystem.
 *
 * Procedure of joining endponts must work very fast however dynamic connection
 * creation upon request cause long and unpredictable delays. Preallocated
 * connection objects gives better result.
 *
 *
 * @author yulian oifa
 */
public class LocalToRemoteConnections extends Connections {
    public LocalToRemoteConnections(BaseEndpointImpl endpoint, int localPoolSize, int rtpPoolSize) throws Exception {
    	super(endpoint,localPoolSize,rtpPoolSize,true);  
    	
    }    

    String key;
    //connecting only local connections to remote and vise versa
    protected void addToConference(BaseConnection connection) {
    	BaseConnection c;
    	LocalChannel channel,channel2;
    	if(connection instanceof LocalConnectionImpl)
    	{
    		for(Enumeration<String> e = activeConnections.keys() ; e.hasMoreElements() ;) {
    			key=e.nextElement();
        		c=activeConnections.get(key);        
        		if (c!=null && c instanceof RtpConnectionImpl && connection != c) {
        			channel = new LocalChannel();
        			channel2=localChannels.putIfAbsent(getChannelId(c,connection),channel);
        			if(channel2==null)
        				channel.join(c, connection);        			
        		}
            }
    	}
    	else
    	{
    		for(Enumeration<String> e = activeConnections.keys() ; e.hasMoreElements() ;) {
    			key=e.nextElement();
        		c=activeConnections.get(key);
        		if (c!=null && c instanceof LocalConnectionImpl && connection != c) {
        			channel = new LocalChannel();
        			channel2=localChannels.putIfAbsent(getChannelId(c,connection),channel);
        			if(channel2==null)
        				channel.join(c, connection);         
        		}        		
            }
    	}    	
    }   
    
    public void updateMode(MediaType mediaType)
    {
    	//only channels should be modified
    }
}
