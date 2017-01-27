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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.NotificationRequest;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.connection.MgcpCall;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.listener.MgcpCallListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.pkg.MgcpEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;
import org.mobicents.media.control.mgcp.pkg.SignalType;

/**
 * Abstract representation of an MGCP Endpoint that groups connections by calls.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEndpoint implements MgcpEndpoint, MgcpCallListener {

    private static final Logger log = Logger.getLogger(GenericMgcpEndpoint.class);

    // MGCP Components
    private final MgcpConnectionProvider connectionProvider;
    protected final MediaGroup mediaGroup;

    // Endpoint Properties
    private final EndpointIdentifier endpointId;
    private final ConcurrentHashMap<Integer, MgcpCall> calls;

    // Endpoint State
    private final AtomicBoolean active;

    // Events and Signals
    private NotifiedEntity notifiedEntity;
    private ConcurrentHashMap<String, MgcpSignal> signals;
    private MgcpRequestedEvent[] requestedEvents;

    // Observers
    private final Collection<MgcpEndpointObserver> endpointObservers;
    private final Collection<MgcpMessageObserver> messageObservers;

    public GenericMgcpEndpoint(EndpointIdentifier endpointId, MgcpConnectionProvider connectionProvider, MediaGroup mediaGroup) {
        // MGCP Components
        this.connectionProvider = connectionProvider;

        // Endpoint Properties
        this.endpointId = endpointId;
        this.calls = new ConcurrentHashMap<>(10);

        // Endpoint State
        this.active = new AtomicBoolean(false);

        // Media Components
        this.mediaGroup = mediaGroup;
        
        // Events and Signals
        this.notifiedEntity = new NotifiedEntity();
        this.signals = new ConcurrentHashMap<>(5);

        // Observers
        this.endpointObservers = new CopyOnWriteArrayList<>();
        this.messageObservers = new CopyOnWriteArrayList<>();
    }

    @Override
    public EndpointIdentifier getEndpointId() {
        return this.endpointId;
    }

    @Override
    public MediaGroup getMediaGroup() {
        return this.mediaGroup;
    }

    public boolean hasCalls() {
        return !this.calls.isEmpty();
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
        
        if(log.isDebugEnabled()) {
            log.debug("Endpoint " + this.endpointId.toString() + " is registering connection " + connection.getHexIdentifier() + " to call " + callId);
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
    public MgcpConnection createConnection(int callId, boolean local) {
        MgcpConnection connection = local ? this.connectionProvider.provideLocal() : this.connectionProvider.provideRemote();
        registerConnection(callId, connection);
        if (!connection.isLocal()) {
            connection.observe(this);
        }
        return connection;
    }

    @Override
    public MgcpConnection deleteConnection(int callId, int connectionId) throws MgcpCallNotFoundException, MgcpConnectionNotFound {
        MgcpCall call = this.calls.get(callId);
        if (call == null) {
            throw new MgcpCallNotFoundException("Call " + callId + " was not found.");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Endpoint " + this.endpointId.toString() + " is unregistering connection "
                        + Integer.toString(connectionId, 16) + " from call " + callId);
            }
            
            // Unregister connection
            MgcpConnection connection = call.removeConnection(connectionId);

            if (connection == null) {
                throw new MgcpConnectionNotFound("Connection " + Integer.toHexString(connectionId) + " was not found in call " + callId);
            } else {
                // Unregister call if it contains no more connections
                if (!call.hasConnections()) {
                    this.calls.remove(callId);
                    if (log.isDebugEnabled()) {
                        log.debug("Endpoint " + this.endpointId.toString() + " unregistered call " + callId + ". Count: " + this.calls.size());
                    }
                }

                // Warn child class that connection was deleted
                onConnectionDeleted(connection);

                // Set endpoint state
                if (!hasCalls()) {
                    deactivate();
                }
                
                // Unregister from connection and close it
                try {
                    connection.forget(this);
                    connection.close();
                } catch (MgcpConnectionException e) {
                    log.error(this.endpointId + ": Connection " + connection.getHexIdentifier() + " was not closed properly", e);
                }

                return connection;
            }
        }
    }

    private List<MgcpConnection> deleteConnections(MgcpCall call) {
        List<MgcpConnection> connections = call.removeConnections();
        for (MgcpConnection connection : connections) {
            // Unregister from connection and close it
            try {
                connection.forget(this);
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
            
            if(log.isDebugEnabled()) {
                log.debug("Endpoint " + this.endpointId.toString() + " deleted " + connections.size() + " connections from call " + callId + ". Call count: " + this.calls.size());
            }

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
        
        if(log.isDebugEnabled()) {
            log.debug("Call " + call.getId() + " terminated on endpoint " + this.endpointId.toString() +". Call count: " + this.calls.size());
        }
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
            
            if(log.isDebugEnabled()) {
                log.debug("Endpoint " + this.endpointId.toString() + " is active.");
            }
            
            notify(this, MgcpEndpointState.ACTIVE);
        }
    }

    protected void deactivate() throws IllegalStateException {
        if (this.active.get()) {
            this.active.set(false);
            onDeactivated();
            
            if(log.isDebugEnabled()) {
                log.debug("Endpoint " + this.endpointId.toString() + " is inactive.");
            }
            
            notify(this, MgcpEndpointState.INACTIVE);
        } else {
            throw new IllegalArgumentException("Endpoint " + this.endpointId + " is already inactive.");
        }
    }

    @Override
    public synchronized void requestNotification(NotificationRequest request) {
        // Update Notified Entity (IF required)
        if (request.getNotifiedEntity() != null) {
            this.notifiedEntity = request.getNotifiedEntity();
        }
        
        // Update registered events
        this.requestedEvents = request.getRequestedEvents();

        /*
         * https://tools.ietf.org/html/rfc3435#section-2.3.4
         * 
         * When a (possibly empty) list of signal(s) is supplied, this list completely replaces the current list of active
         * time-out signals.
         * 
         * Currently active time-out signals that are not provided in the new list MUST be stopped and the new signal(s)
         * provided will now become active.
         * 
         * Currently active time-out signals that are provided in the new list of signals MUST remain active without
         * interruption, thus the timer for such time-out signals will not be affected. Consequently, there is currently no way
         * to restart the timer for a currently active time-out signal without turning the signal off first.
         * 
         * If the time-out signal is parameterized, the original set of parameters MUST remain in effect, regardless of what
         * values are provided subsequently. A given signal MUST NOT appear more than once in a SignalRequests.
         */
        if (request.countSignals() == 0) {
            // List is empty. Cancel all ongoing events.
            Iterator<String> keys = this.signals.keySet().iterator();
            while (keys.hasNext()) {
                MgcpSignal ongoing = this.signals.get(keys.next());
                if (ongoing != null) {
                    ongoing.cancel();
                }
            }
        } else {
            // Execute signals listed in RQNT and cancel remaining
            List<String> retained = new ArrayList<>(request.countSignals());
            for (MgcpSignal signal = request.pollSignal(); signal != null; signal = request.pollSignal()) {
                final SignalType signalType = signal.getSignalType();
                switch (signalType) {
                    case TIME_OUT:
                        // Mark this key to retain ongoing signals
                        String signalName = signal.getName();
                        retained.add(signalName);
                        
                        // Register and execute signal IF NOT duplicate
                        MgcpSignal original = this.signals.putIfAbsent(signalName, signal);
                        if(original == null) {
                            signal.observe(this);
                            signal.execute();
                        }
                        break;

                    case BRIEF:
                        // Brief signals can be executed right away and do not need to be queued.
                        // Their execution is fast and do not generate events.
                        signal.execute();
                        break;

                    default:
                        log.warn("Dropping signal " + signal.toString() + " on endpoint " + getEndpointId().toString()
                                + " because signal type " + signalType + "is not supported.");
                        break;
                }
            }
            
            
            // Cancel every ongoing signal that was not retained
            Iterator<String> keys = this.signals.keySet().iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                if(!retained.contains(key)) {
                    cancelSignal(key);
                }
            }
        }
    }

    @Override
    public void cancelSignal(String signal) {
        MgcpSignal ongoing = this.signals.get(signal);
        if(ongoing != null) {
            if (log.isDebugEnabled()) {
                log.debug("Canceling signal " + ongoing.toString() + " on endpoint " + getEndpointId().toString());
            }
            ongoing.cancel();
        }
    }

    /**
     * Event that is called when a new connection is created in the endpoint. <br>
     * <b>To be overridden by subclasses.</b>
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

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.messageObservers.add(observer);
        if (log.isTraceEnabled()) {
            log.trace("Endpoint " + this.endpointId.toString() + " registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.messageObservers.size());
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.messageObservers.remove(observer);
        if (log.isTraceEnabled()) {
            log.trace("Endpoint " + this.endpointId.toString() + " unregistered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.messageObservers.size());
        }
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.messageObservers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = (MgcpMessageObserver) iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

    @Override
    public void onEvent(Object originator, MgcpEvent event) {
        // Verify if endpoint is listening for such event
        final String composedName = event.getPackage() + "/" + event.getSymbol();
        if (isListening(composedName)) {
            // Unregister from current event
            if(originator instanceof MgcpSignal) {
                MgcpSignal signal = (MgcpSignal) originator;
                this.signals.remove(signal.getName());
                
                // Build Notification
                MgcpRequest notify = new MgcpRequest();
                notify.setRequestType(MgcpRequestType.NTFY);
                notify.setTransactionId(0);
                notify.setEndpointId(this.endpointId.toString());
                
                NotifiedEntity entity = signal.getNotifiedEntity();
                if(entity != null) {
                    notify.addParameter(MgcpParameterType.NOTIFIED_ENTITY, this.notifiedEntity.toString());
                }
                notify.addParameter(MgcpParameterType.OBSERVED_EVENT, event.toString());
                notify.addParameter(MgcpParameterType.REQUEST_ID, Integer.toString(signal.getRequestId(), 16));

                // Send notification to call agent
                // TODO hard-coded port in FROM field
                InetSocketAddress from = new InetSocketAddress(this.endpointId.getDomainName(), 2427);
                InetSocketAddress to = new InetSocketAddress(this.notifiedEntity.getDomain(), this.notifiedEntity.getPort());
                notify(this, from, to, notify, MessageDirection.OUTGOING);
            }
        }
    }

    @Override
    public void observe(MgcpEndpointObserver observer) {
        this.endpointObservers.add(observer);
        if (log.isTraceEnabled()) {
            log.trace("Registered MgcpEndpointObserver@" + observer.hashCode() + ". Count: " + this.endpointObservers.size());
        }
    }

    @Override
    public void forget(MgcpEndpointObserver observer) {
        this.endpointObservers.remove(observer);
        if (log.isTraceEnabled()) {
            log.trace("Unregistered MgcpEndpointObserver@" + observer.hashCode() + ". Count: " + this.endpointObservers.size());
        }
    }

    @Override
    public void notify(MgcpEndpoint endpoint, MgcpEndpointState state) {
        Iterator<MgcpEndpointObserver> iterator = this.endpointObservers.iterator();
        while (iterator.hasNext()) {
            MgcpEndpointObserver observer = iterator.next();
            observer.onEndpointStateChanged(this, state);
        }
    }
    
    private boolean isListening(String event) {
        for (MgcpRequestedEvent evt : this.requestedEvents) {
            if (evt.getQualifiedName().equalsIgnoreCase(event)) {
                return true;
            }
        }
        return false;
    }

}
