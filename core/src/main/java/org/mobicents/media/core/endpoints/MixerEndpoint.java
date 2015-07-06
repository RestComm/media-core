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
        
package org.mobicents.media.core.endpoints;

import org.mobicents.media.core.connections.AbstractConnection;
import org.mobicents.media.server.component.audio.MediaMixer;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.ResourceUnavailableException;

/**
 * Generic implementation of an Endpoint that relies on media mixing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MixerEndpoint extends AbstractEndpoint {
    
    private final MediaMixer audioMixer;
        
    public MixerEndpoint(String localName) {
        super(localName, RelayType.MIXER);
        this.audioMixer = new MediaMixer();
    }
    
    @Override
    public Connection createConnection(ConnectionType type, Boolean isLocal) throws ResourceUnavailableException {
        AbstractConnection connection = (AbstractConnection) super.createConnection(type, isLocal);
        audioMixer.addComponent(connection.generateMixerComponent());
        return connection;
    }
    
    @Override
    public void deleteConnection(Connection connection, ConnectionType connectionType) {
        super.deleteConnection(connection, connectionType);
        audioMixer.removeComponent(connection.getId());
    }

    @Override
    public void modeUpdated(ConnectionMode oldMode, ConnectionMode newMode) {
        // TODO Auto-generated method stub
        
    }

}
