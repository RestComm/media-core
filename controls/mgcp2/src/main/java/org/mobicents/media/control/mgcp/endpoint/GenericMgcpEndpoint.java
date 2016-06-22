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

package org.mobicents.media.control.mgcp.endpoint;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.connection.MgcpCall;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.listener.MgcpCallListener;
import org.mobicents.media.control.mgcp.listener.MgcpConnectionListener;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;

/**
 * Abstract representation of an MGCP Endpoint that groups connections by calls.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEndpoint implements MgcpEndpoint, MgcpCallListener, MgcpConnectionListener {

    private static final Logger log = Logger.getLogger(GenericMgcpEndpoint.class);

    // Endpoint Properties
    private final String endpointId;
    private final NotifiedEntity notifiedEntity;
    private final ConcurrentHashMap<Integer, MgcpCall> calls;

    // Endpoint State
    private final AtomicBoolean active;

    // Events and Signals
    private String[] events;
    private MgcpSignal signal;

    public GenericMgcpEndpoint(String endpointId) {
        // Endpoint Properties
        this.endpointId = endpointId;
        this.notifiedEntity = new NotifiedEntity();
        this.calls = new ConcurrentHashMap<>(10);

        // Endpoint State
        this.active = new AtomicBoolean(false);
    }

    @Override
    public String getEndpointId() {
        return this.endpointId;
    }

    public boolean hasCalls() {
        return this.calls.isEmpty();
    }

    @Override
    public MgcpConnection getConnection(int callId, int connectionId) {
        MgcpCall call = this.calls.get(callId);
        return (call == null) ? null : call.getConnection(connectionId);
    }

    private void registerConnection(int callId, MgcpConnection connection) {
        // Retrieve corresponding call
        MgcpCall call = calls.get(callId);
        if (call == null) {
            // Attempt to insert a new call
            call = new MgcpCall(callId);
            MgcpCall oldCall = this.calls.putIfAbsent(callId, call);

            // Drop newly create call and use existing one
            // This is possible because we are working in non-blocking concurrent scenario
            if (oldCall != null) {
                call = oldCall;
            }
        }

        // Store connection under call
        call.addConnection(connection);

        // Warn child class that connection was created
        onConnectionCreated(connection);

        // Activate endpoint on first registered connection
        if (!isActive()) {
            activate();
        }
    }

    @Override
    public void addConnection(int callId, MgcpConnection connection) throws MgcpConnectionException {
        try {
            registerConnection(callId, connection);
            if (!connection.isLocal()) {
                ((MgcpRemoteConnection) connection).setConnectionListener(this);
            }
        } catch (IllegalArgumentException e) {
            throw new MgcpConnectionException(
                    "Could not add connection " + connection.getHexIdentifier() + " to " + this.endpointId, e);
        }
    }

    @Override
    public MgcpConnection deleteConnection(int callId, int connectionId)
            throws MgcpCallNotFoundException, MgcpConnectionNotFound {
        MgcpCall call = this.calls.get(callId);
        if (call == null) {
            throw new MgcpCallNotFoundException("Call " + callId + " was not found.");
        } else {
            // Unregister connection
            MgcpConnection connection = call.removeConnection(connectionId);

            if (connection == null) {
                throw new MgcpConnectionNotFound(
                        "Connection " + Integer.toHexString(connectionId) + " was not found in call " + callId);
            } else {
                // Unregister call if it contains no more connections
                if (call.hasConnections()) {
                    this.calls.remove(callId);
                }

                // Warn child class that connection was deleted
                onConnectionDeleted(connection);

                // Set endpoint state
                if (!hasCalls()) {
                    deactivate();
                }

                // Close connection
                try {
                    connection.close();
                } catch (MgcpConnectionException e) {
                    log.error(this.endpointId + ": Connection " + connection.getHexIdentifier() + " was not closed properly",
                            e);
                }

                return connection;
            }
        }
    }

    private List<MgcpConnection> deleteConnections(MgcpCall call) {
        List<MgcpConnection> connections = call.removeConnections();
        for (MgcpConnection connection : connections) {
            // Close connection
            try {
                connection.close();
            } catch (MgcpConnectionException e) {
                log.error(this.endpointId + ": Connection " + connection.getHexIdentifier() + " was not closed properly", e);

            }
        }
        return connections;
    }

    @Override
    public List<MgcpConnection> deleteConnections(int callId) throws MgcpCallNotFoundException {
        // De-register call from active sessions
        MgcpCall call = this.calls.remove(callId);
        if (call == null) {
            throw new MgcpCallNotFoundException("Call " + callId + " was not found.");
        } else {
            // Delete all connections from call
            List<MgcpConnection> connections = deleteConnections(call);

            // Set endpoint state
            if (!hasCalls()) {
                deactivate();
            }
            return connections;
        }
    }

    @Override
    public List<MgcpConnection> deleteConnections() {
        List<MgcpConnection> connections = new ArrayList<>();
        Iterator<MgcpCall> iterator = this.calls.values().iterator();
        while (iterator.hasNext()) {
            // Remove call from active call list
            MgcpCall call = iterator.next();
            iterator.remove();

            // Close connections
            connections.addAll(deleteConnections(call));
        }

        // Set endpoint state
        if (!hasCalls()) {
            deactivate();
        }
        return connections;
    }

    @Override
    public void onCallTerminated(MgcpCall call) {
        this.calls.remove(call.getId());
    }

    public boolean isActive() {
        return this.active.get();
    }

    private void activate() throws IllegalStateException {
        if (this.active.get()) {
            throw new IllegalArgumentException("Endpoint " + this.endpointId + " is already active.");
        } else {
            this.active.set(true);
            onActivated();
        }
    }

    protected void deactivate() throws IllegalStateException {
        if (this.active.get()) {
            this.active.set(false);
            onDeactivated();
        } else {
            throw new IllegalArgumentException("Endpoint " + this.endpointId + " is already inactive.");
        }
    }

    @Override
    public void onConnectionFailure(MgcpConnection connection) {
        // Find call that holds the connection
        Iterator<MgcpCall> iterator = this.calls.values().iterator();
        while (iterator.hasNext()) {
            MgcpCall call = iterator.next();
            MgcpConnection removed = call.removeConnection(connection.getIdentifier());

            // Found call where connection was contained
            if (removed != null) {
                // Unregister call if it does not contain any connections
                if (!call.hasConnections()) {
                    iterator.remove();
                }

                // Warn child implementations that a connection was deleted
                onConnectionDeleted(connection);

                // Deactivate endpoint if there are no active calls
                if (!hasCalls()) {
                    deactivate();
                }
            }
        }
    }

    private boolean isListening(String event) {
        if (this.events != null) {
            for (String evt : events) {
                if (evt.equalsIgnoreCase(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void execute(MgcpSignal signal) {
        if (this.signal == null) {
            // No signal being executing. Execute new signal immediately
            this.signal = signal;
            this.signal.execute();
        } else {
            // Current signal is identical to newly request signal. Ignore.
            if (this.signal.equals(signal)) {
                log.warn("Endpoint " + this.endpointId + " dropping duplicate signal " + signal.toString());
            } else {
                this.signal.cancel();
                this.signal = signal;
                this.signal.execute();
            }
        }
    }

    @Override
    public void listen(String... events) {
        this.events = events;
    }

    @Override
    public void onMgcpEvent(MgcpEvent event) {
        final String symbol = event.getSymbol();
        if (isListening(symbol)) {
            // TODO send NTFY to NotifiedEntity
        }
    }

    /**
     * Event that is called when a new connection is created in the endpoint. <br>
     * <b>To be overriden by subclasses.</b>
     * 
     * @param connection
     */
    protected void onConnectionCreated(MgcpConnection connection) {
    }

    /**
     * Event that is called when a new connection is deleted in the endpoint. <br>
     * <b>To be overriden by subclasses.</b>
     * 
     * @param connection
     */
    protected void onConnectionDeleted(MgcpConnection connection) {
    }

    /**
     * Event that is called when endpoint becomes active. <br>
     * <b>To be overriden by subclasses.</b>
     * 
     * @param connection
     */
    protected void onActivated() {
    }

    /**
     * Event that is called when endpoint becomes inactive. <br>
     * <b>To be overriden by subclasses.</b>
     * 
     * @param connection
     */
    protected void onDeactivated() {
    }

}
