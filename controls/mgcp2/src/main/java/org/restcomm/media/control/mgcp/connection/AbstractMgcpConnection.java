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

package org.restcomm.media.control.mgcp.connection;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.exception.MalformedMgcpEventRequestException;
import org.restcomm.media.control.mgcp.exception.MgcpEventNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpPackageNotFoundException;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutEvent;
import org.restcomm.media.server.component.audio.AudioComponent;
import org.restcomm.media.server.component.oob.OOBComponent;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

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
    protected final Set<MgcpEventObserver> observers;
    
    // Timers
    protected final ListeningScheduledExecutorService executor;

    protected static final int HALF_OPEN_TIMER = 30;
    protected ListenableFuture<Integer> timerFuture;
    protected final int timeout;
    protected final int halfOpenTimeout;

    public AbstractMgcpConnection(int identifier, int callId,  int halfOpenTimeout, int openTimeout, MgcpEventProvider eventProvider, ListeningScheduledExecutorService executor) {
        // Connection State
        this.identifier = identifier;
        this.callIdentifier = callId;
        this.mode = ConnectionMode.INACTIVE;
        this.state = MgcpConnectionState.CLOSED;
        this.stateLock = new Object();
        
        // Events
        this.eventProvider = eventProvider;
        this.observers = Sets.newConcurrentHashSet();
        
        // Timers
        this.executor = executor;
        this.timerFuture = null;
        this.halfOpenTimeout = halfOpenTimeout;
        this.timeout = openTimeout;
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
        
        if(log().isDebugEnabled()) {
            log().debug("Connection " + getHexIdentifier() + " mode is " + mode.name());
        }
    }

    public void listen(MgcpRequestedEvent event) throws UnsupportedMgcpEventException {
        if (isEventSupported(event)) {
            try {
                // Parse event request
                MgcpEvent mgcpEvent = this.eventProvider.provide(event);

                // Listen for event
                listen(mgcpEvent);
            } catch (MgcpPackageNotFoundException | MgcpEventNotFoundException | MalformedMgcpEventRequestException e) {
                throw new UnsupportedMgcpEventException("MGCP Event " + event.toString() + " is not supported.", e);
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
    
    protected void expireIn(int timeout) {
        if(this.timerFuture != null && !this.timerFuture.isCancelled()) {
            this.timerFuture.cancel(false);
        }
        
        this.timerFuture = this.executor.schedule(new MgcpConnectionTimer(timeout), timeout, TimeUnit.SECONDS);
        Futures.addCallback(this.timerFuture, new MgcpConnectionTimerCallback(), this.executor);
        
        if (log().isDebugEnabled()) {
            log().debug("Connection " + getHexIdentifier() + " set to expire in " + timeout + " seconds");
        }
    }
    
    /**
     * Raises an RTP Timeout event when connection reaches the end of it's life
     * 
     * @author Henrique Rosa (henrique.rosa@telestax.com)
     *
     */
    final class MgcpConnectionTimer implements Callable<Integer> {

        private final int timeout;

        public MgcpConnectionTimer(int timeout) {
            this.timeout = timeout;
        }

        @Override
        public Integer call() {
            AbstractMgcpConnection.this.notify(AbstractMgcpConnection.this, new RtpTimeoutEvent(getIdentifier(), this.timeout));
            return timeout;
        }

    }

    final class MgcpConnectionTimerCallback implements FutureCallback<Integer> {

        @Override
        public void onSuccess(Integer result) {
            if (log().isInfoEnabled()) {
                log().info("Connection " + getHexIdentifier() + " timed out after " + result + " seconds");
            }

            // Close connection if open
            if (!MgcpConnectionState.CLOSED.equals(state)) {
                try {
                    close();
                } catch (Exception e) {
                    log().warn("Could not close connection " + getHexIdentifier() + " in elegant manner after timeout.");
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if(log().isDebugEnabled()) {
                log().debug("Connection " + getHexIdentifier() +" life timer was canceled or failed.");
            }
        }

    }
}
