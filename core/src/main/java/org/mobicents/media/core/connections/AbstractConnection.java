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
import org.mobicents.media.server.component.audio.MixerComponent;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionFailureListener;
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
    protected AbstractConnection(int id, Scheduler scheduler, RelayType relayType) {
        // Connection properties
        this.id = id;
        this.textualId = Integer.toHexString(id);
        this.relayType = relayType;

        // Keep alive mechanism
        this.scheduler = scheduler;
        heartBeat = new HeartBeat();

        // initialize event objects
        this.listeners = new Listeners<ConnectionListener>();
        this.stateEvent = new ConnectionEventImpl(ConnectionEvent.STATE_CHANGE, this);
    }
    
    protected abstract Logger getLogger();

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

    /**
     * Initiates transition from NULL to HALF_OPEN state.
     */
    public void bind() throws Exception {
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

    /**
     * Initiates transition from HALF_OPEN to OPEN state.
     */
    public void join() throws Exception {
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

    /**
     * Initiates transition from any state to state NULL.
     */
    public void close() {
        synchronized (stateMonitor) {
            if (this.currentState != ConnectionState.NULL) {
                this.onClosed();
                setState(ConnectionState.NULL);
            }
        }
    }

    private void fail() {
        synchronized (stateMonitor) {
            if (this.currentState != ConnectionState.NULL) {
                this.onFailed();
                setState(ConnectionState.NULL);
            }
        }
    }

    /**
     * Gets the current mode of this connection.
     * 
     * @return integer constant indicating mode.
     */
    @Override
    public ConnectionMode getMode() {
        return connectionMode;
    }

    /**
     * Modify mode of this connection for all known media types.
     * 
     * @param mode the new mode of the connection.
     */
    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
        if (this.activeEndpoint != null) {
            this.activeEndpoint.modeUpdated(connectionMode, mode);
        }
        this.connectionMode = mode;
    }

    /**
     * Sets connection failure listener.
     */
    @Override
    public abstract void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener);

    /**
     * Called when connection created.
     */
    protected abstract void onCreated() throws Exception;

    /**
     * Called when connected moved to OPEN state.
     * 
     * @throws Exception
     */
    protected abstract void onOpened() throws Exception;

    /**
     * Called when connection is moving from OPEN state to NULL state.
     */
    protected abstract void onClosed();

    /**
     * Called if failure has bean detected during transition.
     */
    protected abstract void onFailed();

    /**
     * Gets whether connection should be bound to local or remote interface.
     * <p>
     * <b>Supported only for RTP connections.</b>
     * </p>
     * 
     * @return boolean value
     */
    @Override
    public boolean getIsLocal() {
        return false;
    }

    /**
     * Sets whether connection should be bound to local or remote interface.
     * <p>
     * <b>Supported only for RTP connections.</b>
     * </p>
     */
    @Override
    public void setIsLocal(boolean isLocal) {
        // do nothing
    }

    protected void releaseConnection(ConnectionType connectionType) {
        if (this.activeEndpoint != null) {
            this.activeEndpoint.deleteConnection(this, connectionType);
        }
        this.activeEndpoint = null;
    }

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

    public abstract MixerComponent getMediaComponent(String mediaType);

}
