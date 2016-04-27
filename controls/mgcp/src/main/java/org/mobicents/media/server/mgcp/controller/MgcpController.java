/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import org.mobicents.media.server.spi.ControlProtocol;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointInstaller;
import org.mobicents.media.server.spi.ServerManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpController implements ServerManager {

    @Override
    public void onStarted(Endpoint endpoint, EndpointInstaller installer) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onStopped(Endpoint endpoint) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public ControlProtocol getControlProtocol() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void activate() throws IllegalStateException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deactivate() throws IllegalStateException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public boolean isActive() {
        // TODO Auto-generated method stub
        return false;
    }

}
