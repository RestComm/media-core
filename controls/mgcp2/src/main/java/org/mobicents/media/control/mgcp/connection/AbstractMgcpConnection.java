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

import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpConnection implements MgcpConnection {

    // Connection State
    private final int identifier;
    private final String hexIdentifier;
    private MgcpConnectionMode mode;
    private volatile MgcpConnectionState state;
    private final Object stateLock;

    public AbstractMgcpConnection(int identifier) {
        // Connection State
        this.identifier = identifier;
        this.hexIdentifier = Integer.toHexString(identifier);
        this.mode = MgcpConnectionMode.INACTIVE;
        this.state = MgcpConnectionState.CLOSED;
        this.stateLock = new Object();
    }

    @Override
    public int getIdentifier() {
        return this.identifier;
    }

    @Override
    public String getHexIdentifier() {
        return this.hexIdentifier;
    }

    @Override
    public MgcpConnectionState getState() {
        synchronized (this.stateLock) {
            return this.state;
        }
    }

    @Override
    public MgcpConnectionMode getMode() {
        return mode;
    }

    @Override
    public void setMode(MgcpConnectionMode mode) throws IllegalStateException {
        synchronized (this.stateLock) {
            if (MgcpConnectionState.CLOSED.equals(this.state)) {
                throw new IllegalStateException("Cannot update mode because connection is closed.");
            }
        }

        // TODO update mode of media components
        this.mode = mode;
    }

    public abstract AudioComponent getAudioComponent();

    public abstract OOBComponent getOutOfBandComponent();
}
