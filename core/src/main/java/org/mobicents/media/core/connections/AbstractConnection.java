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

import java.io.IOException;

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
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.utils.Text;

/**
 * Implements connection's FSM.
 * 
 * @author Oifa Yulian
 */
public abstract class AbstractConnection implements Connection {

    private int id;

    // Identifier of this connection
    private String textualId;

    // scheduler instance
    protected Scheduler scheduler;

    /** FSM current state */
    private volatile ConnectionState state = ConnectionState.NULL;
    private final Object stateMonitor = new Integer(0);

    // connection event listeners
    private Listeners<ConnectionListener> listeners = new Listeners<ConnectionListener>();

    // events
    private ConnectionEvent stateEvent;

    /** Remaining time to live in current state */
    private volatile long ttl;
    private HeartBeat heartBeat;

    private Endpoint activeEndpoint;

    private ConnectionMode connectionMode = ConnectionMode.INACTIVE;
    private static final Logger logger = Logger.getLogger(AbstractConnection.class);

    /**
     * Creates basic connection implementation.
     * 
     * @param id the unique identifier of this connection within endpoint.
     * @param endpoint the endpoint owner of this connection.
     */
    public AbstractConnection(int id, Scheduler scheduler) {
        this.id = id;
        this.textualId = Integer.toHexString(id);

        this.scheduler = scheduler;

        heartBeat = new HeartBeat();

        // initialize event objects
        this.stateEvent = new ConnectionEventImpl(ConnectionEvent.STATE_CHANGE, this);
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
    public ConnectionState getState() {
        synchronized (stateMonitor) {
            return state;
        }
    }

    /**
     * Modifies state of the connection.
     * 
     * @param state the new value for the state.
     */
    private void setState(ConnectionState state) {
        // change state
        this.state = state;
        this.ttl = state.getTimeout() * 10 + 1;

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
            logger.error(e.getMessage(), e);
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
            logger.error(e.getMessage(), e);
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
            if (this.state != ConnectionState.NULL) {
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
            if (this.state == ConnectionState.NULL) {
                throw new IllegalStateException("Connection not bound yet");
            }

            if (this.state == ConnectionState.OPEN) {
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
            if (this.state != ConnectionState.NULL) {
                this.onClosed();
                setState(ConnectionState.NULL);
            }
        }
    }

    private void fail() {
        synchronized (stateMonitor) {
            if (this.state != ConnectionState.NULL) {
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
     * Joins endpoint wich executes this connection with other party.
     * 
     * @param other the connection executed by other party endpoint.
     * @throws IOException
     */
    @Override
    public abstract void setOtherParty(Connection other) throws IOException;

    /**
     * Joins endpoint which executes this connection with other party.
     * 
     * @param descriptor the SDP descriptor of the other party.
     * @throws IOException
     */
    @Override
    public abstract void setOtherParty(byte[] descriptor) throws IOException;

    /**
     * Joins endpoint which executes this connection with other party.
     * 
     * @param descriptor the SDP descriptor of the other party.
     * @throws IOException
     */
    @Override
    public abstract void setOtherParty(Text descriptor) throws IOException;

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
                ttl--;
                if (ttl == 0) {
                    fail();
                } else {
                    scheduler.submitHeatbeat(this);
                }
            }
            return 0;
        }
    }

    public abstract MixerComponent getMixerComponent(String mediaType);

}
