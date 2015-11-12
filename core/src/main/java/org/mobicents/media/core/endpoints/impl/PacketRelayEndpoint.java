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
import org.mobicents.media.core.endpoints.BaseMixerEndpointImpl;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.EndpointType;
import org.mobicents.media.server.spi.MediaType;

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
    public Connection createConnection(ConnectionType type, Boolean isLocal) {
        switch (type) {
            case RTP:
                return super.createConnection(type, isLocal);
            case LOCAL:
            default:
                return null;
                // throw new ResourceUnavailableException("Local connection is not available on packet relay");
        }
    }

	@Override
    public Component getResource(MediaType mediaType, ComponentType componentType) {
    	return null;
    }

    @Override
    public EndpointType getType() {
        return EndpointType.RELAY;
    }

    @Override
    public void checkIn() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void checkOut() {
        // TODO Auto-generated method stub
        
    }
	
}
