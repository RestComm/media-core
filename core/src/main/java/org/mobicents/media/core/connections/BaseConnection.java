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
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionEvent;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.listener.Listeners;
import org.mobicents.media.server.spi.listener.TooManyListenersException;

/**
 * Implements connection's FSM.
 * 
 * @author Oifa Yulian
 */
public abstract class BaseConnection implements Connection {

    // Task Scheduling
    private final PriorityQueueScheduler scheduler;

    // Connection Properties
    protected final int id;
	protected final String textualId;
	protected ConnectionMode connectionMode;
	protected Endpoint endpoint;

	// FSM current state
	private volatile ConnectionState state;
	private final Object stateMonitor;

	// Connection Event Listeners
	private final Listeners<ConnectionListener> listeners;
	private ConnectionEvent stateEvent;
	protected ConnectionFailureListener connectionFailureListener; 

	// Remaining time to live in current state
	private volatile long ttl;
	private final HeartBeat heartBeat;


	/**
	 * Creates basic connection implementation.
	 * 
	 * @param id
	 *            the unique identifier of this connection within endpoint.
	 * @param endpoint
	 *            the endpoint owner of this connection.
	 */
	public BaseConnection(int id, PriorityQueueScheduler scheduler) {
	    // Task Scheduling
	    this.scheduler = scheduler;

	    // Connection properties
	    this.id = id;
		this.textualId = Integer.toHexString(id);
		this.connectionMode = ConnectionMode.INACTIVE;
		
		// FSM current state
		this.state = ConnectionState.NULL;
		this.stateMonitor = new Integer(0);

		// Keep alive mechanism
		this.heartBeat = new HeartBeat();

		// Connection Event Listeners
		this.listeners = new Listeners<ConnectionListener>();
		this.stateEvent = new ConnectionEventImpl(ConnectionEvent.STATE_CHANGE, this);
	}
	
	protected abstract Logger getLogger();

	public abstract AudioComponent getAudioComponent();

	public abstract OOBComponent getOOBComponent();

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
	 * @param state
	 *            the new value for the state.
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
			getLogger().error(e.getMessage(), e);
		}
	}

	@Override
	public void setEndpoint(Endpoint endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public Endpoint getEndpoint() {
		return this.endpoint;
	}
	
    @Override
    public void setConnectionFailureListener(ConnectionFailureListener listener) {
        this.connectionFailureListener = listener;
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
	public void bind() throws IllegalStateException {
		synchronized (stateMonitor) {
			// check current state
			if (this.state != ConnectionState.NULL) {
				throw new IllegalStateException("Connection already bound");
			}

			// update state
			setState(ConnectionState.HALF_OPEN);
		}
	}

	/**
	 * Initiates transition from HALF_OPEN to OPEN state.
	 */
	public void open() throws IllegalStateException {
		synchronized (stateMonitor) {
			if (this.state == ConnectionState.NULL) {
				throw new IllegalStateException("Connection not bound yet");
			}

			if (this.state == ConnectionState.OPEN) {
				throw new IllegalStateException("Connection opened already");
			}

			// update state
			setState(ConnectionState.OPEN);
		}
	}

	@Override
	public void close() throws IllegalStateException {
		synchronized (stateMonitor) {
			if (this.state != ConnectionState.NULL) {
				this.onClosed();
				setState(ConnectionState.NULL);
				this.endpoint = null;
			}
		}
	}

	private void fail() {
		synchronized (stateMonitor) {
			if (this.state != ConnectionState.NULL) {
			    getLogger().warn("Connection " + this.id + " failed.");
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
	 * @param mode
	 *            the new mode of the connection.
	 */
	@Override
	public void setMode(ConnectionMode mode) throws ModeNotSupportedException {
		if (this.endpoint != null) {
			this.endpoint.modeUpdated(connectionMode, mode);
		}
		this.connectionMode = mode;
	}

	/**
	 * Called when connection is moving from OPEN state to NULL state.
	 */
	protected abstract void onClosed();

	/**
	 * Called if failure has bean detected during transition.
	 */
	protected abstract void onFailed();
	
    @Override
    public String toString() {
        return getType() + "-connection-" + this.id;
    }

	private class HeartBeat extends Task {

		public HeartBeat() {
			super();
		}

		@Override
		public int getQueueNumber() {
			return PriorityQueueScheduler.HEARTBEAT_QUEUE;
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

}
