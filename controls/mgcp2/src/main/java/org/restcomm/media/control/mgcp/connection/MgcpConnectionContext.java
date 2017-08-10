/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.connection;

import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpConnectionContext {
    
    protected static final int HALF_OPEN_TIMEOUT = 30;
    protected static final int NO_TIMEOUT = -1;
    

    // Connection
    private final int identifier;
    private final int callIdentifier;
    private ConnectionMode mode;
    
    // Timers
    protected final int timeout;
    protected final int halfOpenTimeout;

    public MgcpConnectionContext(int identifier, int callIdentifier, int halfOpenTimeout, int timeout) {
        // Connection
        this.identifier = identifier;
        this.callIdentifier = callIdentifier;
        this.mode = ConnectionMode.INACTIVE;
        
        // Timers
        this.halfOpenTimeout = halfOpenTimeout;
        this.timeout = timeout;
    }

    public MgcpConnectionContext(int identifier, int callIdentifier, int timeout) {
        this(identifier, callIdentifier, HALF_OPEN_TIMEOUT, timeout);
    }

    public MgcpConnectionContext(int identifier, int callIdentifier) {
        this(identifier, callIdentifier, NO_TIMEOUT);
    }

    public ConnectionMode getMode() {
        return mode;
    }

    public void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    public int getIdentifier() {
        return identifier;
    }
    
    public String getHexIdentifier() {
        return Integer.toHexString(identifier).toUpperCase();
    }

    public int getCallIdentifier() {
        return callIdentifier;
    }
    
    public String getCallIdentifierHex() {
        return Integer.toHexString(this.callIdentifier).toUpperCase();
    }
    
    public int getTimeout() {
        return timeout;
    }
    
    public int getHalfOpenTimeout() {
        return halfOpenTimeout;
    }

}
