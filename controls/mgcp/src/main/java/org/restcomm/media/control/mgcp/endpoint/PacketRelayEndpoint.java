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

package org.restcomm.media.control.mgcp.endpoint;

import org.restcomm.media.Component;
import org.restcomm.media.ComponentType;
import org.restcomm.media.spi.Connection;
import org.restcomm.media.spi.ConnectionType;
import org.restcomm.media.spi.MediaType;
import org.restcomm.media.spi.ResourceUnavailableException;

/**
 * Packet Relay Endpoint Implementation
 * 
 * @author yulian oifa
 */
public class PacketRelayEndpoint extends BaseMixerEndpointImpl {
    
	public PacketRelayEndpoint(String localName) {
    	super(localName);              
    }

	@Override
    public Connection createConnection(ConnectionType type,Boolean isLocal) throws ResourceUnavailableException {
    	switch(type) {
    		case RTP:
    			return super.createConnection(type,isLocal);    			
    		case LOCAL:
    			throw new ResourceUnavailableException("Local connection is not available on packet relay");    			
    	}
    	return null;
    }

	@Override
    public Component getResource(MediaType mediaType, ComponentType componentType) {
    	return null;
    }
	
}
