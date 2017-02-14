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
import org.mobicents.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpEventProvider;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.LocalDataChannel;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ModeNotSupportedException;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Type of connection that connects two endpoints locally.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnection extends AbstractMgcpConnection {

    private static final Logger log = Logger.getLogger(MgcpLocalConnection.class);

    private final LocalDataChannel audioChannel;

    public MgcpLocalConnection(int identifier, int callId, int halfOpenTimeout, int openTimeout, MgcpEventProvider eventProvider, ChannelsManager channelProvider, ListeningScheduledExecutorService executor) {
        super(identifier, callId, halfOpenTimeout, openTimeout, eventProvider, executor);
        this.audioChannel = channelProvider.getLocalChannel();
    }
    
    public MgcpLocalConnection(int identifier, int callId, int timeout, MgcpEventProvider eventProvider, ChannelsManager channelProvider, ListeningScheduledExecutorService executor) {
        this(identifier, callId, HALF_OPEN_TIMER, timeout, eventProvider, channelProvider, executor);
    }

    public MgcpLocalConnection(int identifier, int callId, MgcpEventProvider eventProvider, ChannelsManager channelProvider, ListeningScheduledExecutorService executor) {
        this(identifier, callId, HALF_OPEN_TIMER, 0, eventProvider, channelProvider, executor);
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
                    
                    if (this.halfOpenTimeout > 0) {
                        expireIn(this.halfOpenTimeout);
                    }
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
                    
                    // Submit timer
                    if (this.timeout > 0) {
                        expireIn(this.timeout);
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
                    
                    // Cancel timer
                    if(this.timerFuture != null && !this.timerFuture.isDone()) {
                        this.timerFuture.cancel(false);
                    }

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
    protected boolean isEventSupported(MgcpRequestedEvent event) {
        return false;
    }
    
    @Override
    protected void listen(MgcpEvent event) throws UnsupportedMgcpEventException {
        if (log.isDebugEnabled()) {
            log.debug("Connection " + getCallIdentifierHex() + " is listening to event " + event.toString());
        }
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
