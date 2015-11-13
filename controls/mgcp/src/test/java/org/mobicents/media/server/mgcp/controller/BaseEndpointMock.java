/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag. 
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
        
package org.mobicents.media.server.mgcp.controller;

import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.EndpointType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BaseEndpointMock extends BaseEndpointImpl {

    public BaseEndpointMock(String localName) {
        super(localName);
    }

    @Override
    public EndpointType getType() {
        return EndpointType.BRIDGE;
    }

    @Override
    public void deleteConnection(int connectionId) {
        this.connections.remove(connectionId);
    }
    
    @Override
    public void start() throws ResourceUnavailableException {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        // TODO Auto-generated method stub
        
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
