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
        
package org.mobicents.media.core.call;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.core.connections.MyTestConnection;
import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.EndpointType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MockEndpoint extends BaseEndpointImpl {
    
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);
    private static final AtomicInteger CONNECTION_ID_GENERATOR = new AtomicInteger(0);
    
    public MockEndpoint() {
        super("mobicents/mock/" + ID_GENERATOR.incrementAndGet());
    }
    
    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) {
        MyTestConnection connection = new MyTestConnection(CONNECTION_ID_GENERATOR.incrementAndGet(), null);
        Connection value = connections.putIfAbsent(connection.getId(), connection);
        return value == null ? connection : value;
    }

    @Override
    public void deleteConnection(int connectionId) {
        this.connections.remove(connectionId);
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public EndpointType getType() {
        // TODO Auto-generated method stub
        return null;
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
