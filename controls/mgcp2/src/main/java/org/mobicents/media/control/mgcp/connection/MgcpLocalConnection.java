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

package org.mobicents.media.control.mgcp.connection;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.LocalDataChannel;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ModeNotSupportedException;

/**
 * Type of connection that connects two endpoints locally.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnection extends AbstractMgcpConnection {

    private static final Logger log = Logger.getLogger(MgcpLocalConnection.class);

    private final LocalDataChannel audioChannel;

    public MgcpLocalConnection(int identifier, ChannelsManager channelProvider) {
        super(identifier);
        this.audioChannel = channelProvider.getLocalChannel();
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public String halfOpen(LocalConnectionOptions options) throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case CLOSED:
                    this.state = MgcpConnectionState.HALF_OPEN;
                    break;

                default:
                    throw new MgcpConnectionException("Cannot half-open connection " + this.getHexIdentifier()
                            + " because state is " + this.state.name());
            }
        }
        return null;
    }

    @Override
    public String open(String sdp) throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case CLOSED:
                case HALF_OPEN:
                    this.state = MgcpConnectionState.OPEN;
                    
                    if(log.isDebugEnabled()) {
                        log.debug("Connection " + getHexIdentifier() + " state is " + this.state.name());
                    }
                    
                    break;

                default:
                    throw new MgcpConnectionException(
                            "Cannot open connection " + this.getHexIdentifier() + " because state is " + this.state.name());
            }
        }
        return null;
    }

    public void join(MgcpLocalConnection otherConnection) throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case OPEN:
                    try {
                        this.audioChannel.join(otherConnection.audioChannel);
                    } catch (IOException e) {
                        throw new MgcpConnectionException("Cannot join connection " + this.getHexIdentifier()
                                + " to connection " + otherConnection.getHexIdentifier(), e);
                    }
                    break;

                default:
                    throw new MgcpConnectionException("Cannot join connection " + this.getHexIdentifier() + " to connection "
                            + otherConnection.getHexIdentifier() + " because current state is not OPEN");
            }
        }
    }

    @Override
    public void close() throws MgcpConnectionException {
        synchronized (this.stateLock) {
            switch (this.state) {
                case HALF_OPEN:
                case OPEN:
                    // Deactivate connection
                    setMode(ConnectionMode.INACTIVE);

                    // Update connection state
                    this.state = MgcpConnectionState.CLOSED;

                    // Close audio channel
                    this.audioChannel.unjoin();
                    
                    if(log.isDebugEnabled()) {
                        log.debug("Connection " + getHexIdentifier() + " state is " + this.state.name());
                    }
                    break;

                default:
                    throw new MgcpConnectionException(
                            "Cannot close connection " + this.getHexIdentifier() + " because state is " + this.state.name());
            }
        }
    }

    @Override
    public void setMode(ConnectionMode mode) throws IllegalStateException {
        try {
            this.audioChannel.updateMode(mode);
            
            if(log.isDebugEnabled()) {
                log.debug("Connection " + getHexIdentifier() + " mode is " + mode.name());
            }
        } catch (ModeNotSupportedException e) {
            log.warn("Could not update data channel mode of local connection " + this.getHexIdentifier());
        }
        super.setMode(mode);
    }

    @Override
    public AudioComponent getAudioComponent() {
        return this.audioChannel.getAudioComponent();
    }

    @Override
    public OOBComponent getOutOfBandComponent() {
        return this.audioChannel.getOOBComponent();
    }
    
    @Override
    protected Logger log() {
        return log;
    }

}
