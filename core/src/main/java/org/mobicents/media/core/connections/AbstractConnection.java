/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.mobicents.media.core.connections;

import org.apache.log4j.Logger;
import org.mobicents.media.server.component.audio.MediaComponent;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.RelayType;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 * Implements connection's FSM.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public abstract class AbstractConnection implements Connection {

    // Connection properties
    private int id;
    private String textualId;
    private RelayType relayType;
    private final ConnectionType connectionType;
    private ConnectionMode connectionMode = ConnectionMode.INACTIVE;
    private Endpoint activeEndpoint;

    // Finite state machine
    private volatile ConnectionState currentState = ConnectionState.NULL;
    private final Object stateMonitor = new Integer(0);

    // connection event listeners
    private final Listeners<ConnectionListener> listeners;
    private ConnectionEvent stateEvent;

    // Keep alive mechanism
    protected Scheduler scheduler;
    private volatile long timeToLive;
    private HeartBeat heartBeat;

    /**
     * Creates basic connection implementation.
     * 
     * @param id the unique identifier of this connection within endpoint.
     * @param endpoint the endpoint owner of this connection.
     */
    protected AbstractConnection(int id, Scheduler scheduler, RelayType relayType, ConnectionType connectionType) {
        // Connection properties
        this.id = id;
        this.textualId = Integer.toHexString(id);
        this.relayType = relayType;
        this.connectionType = connectionType;

        // Keep alive mechanism
        this.scheduler = scheduler;
        heartBeat = new HeartBeat();

        // initialize event objects
        this.listeners = new Listeners<ConnectionListener>();
        this.stateEvent = new ConnectionEventImpl(ConnectionEvent.STATE_CHANGE, this);
    }

    protected AbstractConnection(int id, Scheduler scheduler, ConnectionType connectionType) {
        this(id, scheduler, RelayType.MIXER, connectionType);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getTextualId() {
        return textualId;
    }

    @Override
    public RelayType getRelayType() {
        return relayType;
    }

    @Override
    public void setRelayType(RelayType relayType) {
        this.relayType = relayType;
    }

    @Override
    public ConnectionType getConnectionType() {
        return this.connectionType;
    }

    @Override
    public ConnectionState getState() {
        synchronized (stateMonitor) {
            return currentState;
        }
    }

    /**
     * Modifies state of the connection.
     * 
     * @param state the new value for the state.
     */
    private void setState(ConnectionState state) {
        // change state
        this.currentState = state;
        this.timeToLive = state.getTimeout() * 10 + 1;

        switch (state) {
            case HALF_OPEN:
                scheduler.submitHeatbeat(heartBeat);
                break;
            case NULL:
                heartBeat.cancel();
                break;
            default:
                break;
        }

        // notify listeners
        try {
            listeners.dispatch(stateEvent);
        } catch (Exception e) {
            getLogger().error(e.getMessage(), e);
        }
    }

    @Override
    public String getDescriptor() {
        return null;
    }

    @Override
    public String getLocalDescriptor() {
        return null;
    }

    @Override
    public String getRemoteDescriptor() {
        return null;
    }

    @Override
    public void setEndpoint(Endpoint endpoint) {
        this.activeEndpoint = endpoint;
    }

    @Override
    public Endpoint getEndpoint() {
        return this.activeEndpoint;
    }

    @Override
    public void addListener(ConnectionListener listener) {
        try {
            listeners.add(listener);
        } catch (TooManyListenersException e) {
            getLogger().error(e.getMessage(), e);
        }
    }

    @Override
    public void removeListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void halfOpen() throws IllegalStateException {
        synchronized (stateMonitor) {
            // check current state
            if (this.currentState != ConnectionState.NULL) {
                throw new IllegalStateException("Connection already bound");
            }

            // execute call back
            this.onCreated();

            // update state
            setState(ConnectionState.HALF_OPEN);
        }
    }

    @Override
    public void open() throws IllegalStateException {
        synchronized (stateMonitor) {
            if (this.currentState == ConnectionState.NULL) {
                throw new IllegalStateException("Connection not bound yet");
            }

            if (this.currentState == ConnectionState.OPEN) {
                throw new IllegalStateException("Connection opened already");
            }

            // execute callback
            this.onOpened();

            // update state
            setState(ConnectionState.OPEN);
        }
    }

    @Override
    public void close() {
        synchronized (stateMonitor) {
            if (this.currentState != ConnectionState.NULL) {
                this.onClosed();
                setState(ConnectionState.NULL);
                this.activeEndpoint = null;
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.currentState.equals(ConnectionState.NULL);
    }

    private void fail() {
        synchronized (stateMonitor) {
            if (this.currentState != ConnectionState.NULL) {
                this.onFailed();
                setState(ConnectionState.NULL);
                this.activeEndpoint = null;
            }
        }
    }

    @Override
    public ConnectionMode getMode() {
        return connectionMode;
    }

    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
        if (this.activeEndpoint != null) {
            this.activeEndpoint.modeUpdated(connectionMode, mode);
        }
        this.connectionMode = mode;
    }

    @Override
    public boolean getIsLocal() {
        return false;
    }

    @Override
    public void setIsLocal(boolean isLocal) {
        // do nothing
    }

    protected abstract Logger getLogger();

    public abstract MediaComponent getMediaComponent(String mediaType);

    /**
     * Called when connection created.
     */
    protected abstract void onCreated();

    /**
     * Called when connected moved to OPEN state.
     */
    protected abstract void onOpened();

    /**
     * Called when connection is moving from OPEN state to NULL state.
     */
    protected abstract void onClosed();

    /**
     * Called if failure has bean detected during transition.
     */
    protected abstract void onFailed();

    private class HeartBeat extends Task {

        public HeartBeat() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return Scheduler.HEARTBEAT_QUEUE;
        }

        @Override
        public long perform() {
            synchronized (stateMonitor) {
                timeToLive--;
                if (timeToLive == 0) {
                    fail();
                } else {
                    scheduler.submitHeatbeat(this);
                }
            }
            return 0;
        }
    }

}
