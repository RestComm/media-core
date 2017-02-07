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

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.exception.MalformedMgcpEventRequestException;
import org.mobicents.media.control.mgcp.exception.MgcpEventNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpPackageNotFoundException;
import org.mobicents.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpEventObserver;
import org.mobicents.media.control.mgcp.pkg.MgcpEventProvider;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.spi.ConnectionMode;

import com.google.common.collect.Sets;

/**
 * Base implementation for any MGCP connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpConnection implements MgcpConnection {

    // Connection State
    private final int identifier;
    private final int callIdentifier;
    private ConnectionMode mode;
    protected volatile MgcpConnectionState state;
    protected final Object stateLock;
    
    // Events
    private final MgcpEventProvider eventProvider;
    protected final ConcurrentMap<String, MgcpEvent> events;
    protected final Set<MgcpEventObserver> observers;

    public AbstractMgcpConnection(int identifier, int callId, MgcpEventProvider eventProvider) {
        // Connection State
        this.identifier = identifier;
        this.callIdentifier = callId;
        this.mode = ConnectionMode.INACTIVE;
        this.state = MgcpConnectionState.CLOSED;
        this.stateLock = new Object();
        
        // Events
        this.eventProvider = eventProvider;
        this.events = new ConcurrentHashMap<>();
        this.observers = Sets.newConcurrentHashSet();
    }

    @Override
    public int getIdentifier() {
        return this.identifier;
    }

    @Override
    public String getHexIdentifier() {
        return Integer.toHexString(identifier).toUpperCase();
    }

    @Override
    public int getCallIdentifier() {
        return this.callIdentifier;
    }
    
    @Override
    public String getCallIdentifierHex() {
        return Integer.toHexString(this.callIdentifier).toUpperCase();
    }

    @Override
    public ConnectionMode getMode() {
        return mode;
    }
    
    @Override
    public MgcpConnectionState getState() {
        return this.state;
    }

    @Override
    public void setMode(ConnectionMode mode) throws IllegalStateException {
        synchronized (this.stateLock) {
            if (MgcpConnectionState.CLOSED.equals(this.state)) {
                throw new IllegalStateException("Cannot update mode because connection is closed.");
            }
        }
        this.mode = mode;
    }
    
    public void listen(MgcpRequestedEvent event) throws UnsupportedMgcpEventException {
        if(isEventSupported(event)) {
            // Parse event request
            MgcpEvent mgcpEvent;
            try {
                mgcpEvent = this.eventProvider.provide(event);
            } catch (MgcpPackageNotFoundException | MgcpEventNotFoundException | MalformedMgcpEventRequestException e) {
                throw new UnsupportedMgcpEventException("MGCP Event " + event.toString() + " is not supported.", e);
            }
            
            // Listen for requested event
            MgcpEvent old = this.events.putIfAbsent(event.getQualifiedName(), mgcpEvent);
            if(old == null) {
                listen(mgcpEvent);
            } else {
                // TODO override event
            }
        } else {
            // Event not supported
            throw new UnsupportedMgcpEventException("Connection " + getCallIdentifierHex() + " does not support event " + event.getQualifiedName());
        }
    }

    protected abstract boolean isEventSupported(MgcpRequestedEvent event);
    
    protected abstract void listen(MgcpEvent event) throws UnsupportedMgcpEventException;
    
    public abstract AudioComponent getAudioComponent();

    public abstract OOBComponent getOutOfBandComponent();

    @Override
    public void observe(MgcpEventObserver observer) {
        boolean added = this.observers.add(observer);
        if (added && log().isTraceEnabled()) {
            log().trace("Connection " + getHexIdentifier() + " registered MgcpEventObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(MgcpEventObserver observer) {
        boolean removed = this.observers.remove(observer);
        if (removed && log().isTraceEnabled()) {
            log().trace("Connection " + getHexIdentifier() + " unregistered MgcpEventObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(Object originator, MgcpEvent event) {
        Iterator<MgcpEventObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpEventObserver observer = (MgcpEventObserver) iterator.next();
            if(observer != originator) {
                observer.onEvent(originator, event);
            }
        }
    }
    
    protected abstract Logger log();
    
    // TODO implement heart beat
}
