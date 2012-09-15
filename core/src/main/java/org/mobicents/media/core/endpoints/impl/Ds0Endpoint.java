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

package org.mobicents.media.core.endpoints.impl;

import org.mobicents.media.Component;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.core.endpoints.BaseSS7EndpointImpl;
import org.mobicents.media.core.endpoints.MediaGroup;
import org.mobicents.media.server.impl.rtp.ChannelsManager;

/**
 * Ds0 Endpoint Implementation
 * 
 * @author yulian oifa 
 */
public class Ds0Endpoint extends BaseSS7EndpointImpl {
    
	public Ds0Endpoint(String localName,ChannelsManager channelsManager,int channelID,boolean isALaw) {
    	super(localName,channelsManager,channelID,isALaw);              
    }	

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.server.spi.Endpoint#getResource();
     */
    public Component getResource(MediaType mediaType, ComponentType componentType)
    {
    	switch(mediaType)
    	{
    		case AUDIO:
    			switch(componentType)
    			{
    				case SIGNAL_DETECTOR:
    					return mediaGroup.getSignalDetector();    					
    				case SIGNAL_GENERATOR:
    					return mediaGroup.getSignalGenerator();    					
    			}    			
    			break;
    	}
    	
    	return null;
    }        
}
