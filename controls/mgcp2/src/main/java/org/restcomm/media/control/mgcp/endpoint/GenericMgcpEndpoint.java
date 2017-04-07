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

package org.restcomm.media.control.mgcp.endpoint;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.NotificationRequest;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionState;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpSignal;
import org.restcomm.media.control.mgcp.pkg.SignalType;
import org.restcomm.media.control.mgcp.pkg.r.rto.RtpTimeoutEvent;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * Abstract representation of an MGCP Endpoint that groups connections by calls.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GenericMgcpEndpoint implements MgcpEndpoint {

    private static final Logger log = Logger.getLogger(GenericMgcpEndpoint.class);
    
    private static final MgcpRequestedEvent[] EMPTY_ENDPOINT_EVENTS = new MgcpRequestedEvent[0];
    
    // MGCP Components
    private final MgcpConnectionProvider connectionProvider;
    protected final MediaGroup mediaGroup;

    // Endpoint Properties
    private final EndpointIdentifier endpointId;
    private final ConcurrentHashMap<Integer, MgcpConnection> connections;

    // Endpoint State
    private final AtomicBoolean active;

    // Events and Signals
    private NotifiedEntity notifiedEntity;
    private ConcurrentHashMap<String, MgcpSignal> signals;
    // TODO requestedEndpointEvents needs to be synchronized!
    private MgcpRequestedEvent[] requestedEndpointEvents;
    private final Multimap<Integer, MgcpRequestedEvent> requestedConnectionEvents;

    // Observers
    private final Set<MgcpEndpointObserver> endpointObservers;
    private final Set<MgcpMessageObserver> messageObservers;

    public GenericMgcpEndpoint(EndpointIdentifier endpointId, MgcpConnectionProvider connectionProvider, MediaGroup mediaGroup) {
        // MGCP Components
        this.connectionProvider = connectionProvider;

        // Endpoint Properties
        this.endpointId = endpointId;
        this.connections = new ConcurrentHashMap<>(5);

        // Endpoint State
        this.active = new AtomicBoolean(false);

        // Media Components
        this.mediaGroup = mediaGroup;
        
        // Events and Signals
        this.notifiedEntity = new NotifiedEntity();
        this.signals = new ConcurrentHashMap<>(5);
        this.requestedEndpointEvents = EMPTY_ENDPOINT_EVENTS;
        this.requestedConnectionEvents = Multimaps.synchronizedSetMultimap(HashMultimap.<Integer, MgcpRequestedEvent>create());

        // Observers
        this.endpointObservers = Sets.newConcurrentHashSet();
        this.messageObservers = Sets.newConcurrentHashSet();
    }

    @Override
    public EndpointIdentifier getEndpointId() {
        return this.endpointId;
    }

    @Override
    public MediaGroup getMediaGroup() {
        return this.mediaGroup;
    }

    public boolean hasConnections() {
        return !this.connections.isEmpty();
    }

    @Override
    public MgcpConnection getConnection(int callId, int connectionId) {
        MgcpConnection connection = this.connections.get(connectionId);
        if(connection != null && connection.getCallIdentifier() == callId) {
            return connection;
        }
        return null;
    }

    private boolean registerConnection(int callId, MgcpConnection connection) {
        MgcpConnection old = this.connections.putIfAbsent(connection.getIdentifier(), connection);
        boolean registered = (old == null);

        if (registered) {
            if (log.isDebugEnabled()) {
                log.debug("Endpoint " + this.endpointId.toString() + " registered connection " + connection.getHexIdentifier() + " to call " + connection.getCallIdentifierHex());
            }
            
            // Observe connection
            connection.observe(this);

            // Warn child class that connection was created
            onConnectionCreated(connection);

            // Activate endpoint on first registered connection
            if (!isActive()) {
                activate();
            }
        }
        return registered;
    }

    @Override
    public MgcpConnection createConnection(int callId, boolean local) {
        MgcpConnection connection = local ? this.connectionProvider.provideLocal(callId) : this.connectionProvider.provideRemote(callId);
        registerConnection(callId, connection);
        if (!connection.isLocal()) {
            connection.observe(this);
        }
        return connection;
    }

    @Override
    public MgcpConnection deleteConnection(int callId, int connectionId) throws MgcpCallNotFoundException, MgcpConnectionNotFoundException {
        MgcpConnection connection = this.connections.get(connectionId);
        
        if(connection == null) {
            throw new MgcpConnectionNotFoundException(this.endpointId + " could not find connection " + Integer.toHexString(connectionId).toUpperCase() + " in call " + Integer.toHexString(callId).toUpperCase());
        } else if (connection.getCallIdentifier() != callId) {
            throw new MgcpCallNotFoundException(this.endpointId + " could not find connection " + Integer.toHexString(connectionId).toUpperCase() + " in call " + Integer.toHexString(callId).toUpperCase());
        }
        
        connection = this.connections.remove(connectionId);
        
        if (log.isDebugEnabled()) {
            log.debug("Endpoint " + this.endpointId + " unregistered connection " + connection.getHexIdentifier() + " from call " + connection.getCallIdentifierHex());
        }
        
        // Warn child class that connection was deleted
        onConnectionDeleted(connection);

        // Set endpoint state
        if (!hasConnections() && isActive()) {
            deactivate();
        }
        
        // Unregister from connection and close it if necessary
        try {
            connection.forget(this);
            
            if(!MgcpConnectionState.CLOSED.equals(connection.getState())) {
                connection.close();
            }
        } catch (MgcpConnectionException e) {
            log.warn(this.endpointId + " could not close connection " + connection.getHexIdentifier() + " in elegant manner.", e);
        }
        return connection;
    }

    @Override
    public List<MgcpConnection> deleteConnections(int callId) throws MgcpCallNotFoundException {
        // Fetch all current connections
        Collection<MgcpConnection> current = this.connections.values();
        List<MgcpConnection> deleted = new ArrayList<>(current.size());
        
        for (MgcpConnection connection : current) {
            // Delete connection if owned by specific call-id
            if(connection.getCallIdentifier() == callId) {
                MgcpConnection removed = this.connections.remove(connection.getIdentifier());
                if(removed != null) {
                    deleted.add(removed);
                }
            }
        }
        
        // No connections were found for specific call
        if(deleted.size() == 0) {
            throw new MgcpCallNotFoundException(this.endpointId + " could not find call " + Integer.toHexString(callId).toUpperCase());
        }
        
        // Log deleted calls
        if(log.isDebugEnabled()) {
            String hexIdentifiers = Arrays.toString(getConnectionHexId(deleted));
            log.debug("Endpoint " + this.endpointId.toString() + " deleted " + deleted.size() + " connections from call " + callId + ": "+ hexIdentifiers +". Connection count: " + this.connections.size());
        }
        
        // Update endpoint state if all connections were deleted
        if (!hasConnections() && isActive()) {
            deactivate();
        }
        return deleted;
    }

    @Override
    public List<MgcpConnection> deleteConnections() {
        Set<Integer> keys = this.connections.keySet();
        List<MgcpConnection> deleted = new ArrayList<>(keys.size());
        
        for (Integer key : keys) {
            MgcpConnection connection = this.connections.remove(key);
            if(connection != null) {
                // Unregister from connection and close it if needed
                try {
                    connection.forget(this);
                    if(!MgcpConnectionState.CLOSED.equals(connection.getState())) {
                        connection.close();
                    }
                } catch (MgcpConnectionException e) {
                    log.warn(this.endpointId + " could not close connection " + connection.getHexIdentifier() + " in elegant manner.", e);
                }

                // Add connection to list of deleted connections
                deleted.add(connection);
            }
        }
        
        // Log deleted calls
        if(log.isDebugEnabled()) {
            String hexIdentifiers = Arrays.toString(getConnectionHexId(deleted));
            log.debug("Endpoint " + this.endpointId.toString() + " deleted " + deleted.size() + " connections: "+ hexIdentifiers +". Connection count: " + this.connections.size());
        }
        
        // Deactivate endpoint if no connections exist
        if (!hasConnections() && isActive()) {
            deactivate();
        }
        return deleted;
    }
    
    private String[] getConnectionHexId(Collection<MgcpConnection> connections) {
        String[] hex = new String[connections.size()];
        int index = 0;
        for (MgcpConnection connection : connections) {
            hex[index] = connection.getHexIdentifier();
            index++;
        }
        return hex;
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

        // Clear requested events
        this.requestedEndpointEvents = EMPTY_ENDPOINT_EVENTS;
        this.requestedConnectionEvents.clear();
        
        // Update registered events
        int eventCount = request.getRequestedEvents().length;
        List<MgcpRequestedEvent> endpointEvents = new ArrayList<>(eventCount);
        
        for (MgcpRequestedEvent requestedEvent : request.getRequestedEvents()) {
            if(requestedEvent.getConnectionId() > 0) {
                int connectionId = requestedEvent.getConnectionId();
                MgcpConnection connection = this.connections.get(connectionId);
                
                if(connection == null) {
                    log.warn("Requested event " + requestedEvent.toString() + " was dropped because connection " + Integer.toHexString(connectionId) + "was not found.");
                } else {
                    try {
                        // Process connection event
                        connection.listen(requestedEvent);

                        // Register event notification
                        this.requestedConnectionEvents.put(connectionId, requestedEvent);
                        if (log.isDebugEnabled()) {
                            log.debug("Endpoint " + this.endpointId + " requested event " + requestedEvent.getQualifiedName() + " to connection " + requestedEvent.getConnectionId());
                        }
                    } catch (UnsupportedMgcpEventException e) {
                        log.warn("Requested event " + requestedEvent.toString() + " was dropped because it was not supported by connection " + connection.getHexIdentifier(), e);
                    }
                }
            } else {
                // Process endpoint event
                endpointEvents.add(requestedEvent);
                if(log.isDebugEnabled()) {
                    log.debug("Endpoint " + this.endpointId + " is listening for event " + requestedEvent.getQualifiedName());
                }
            }
        }
        
        if(endpointEvents.size() > 0) {
            this.requestedEndpointEvents = endpointEvents.toArray(new MgcpRequestedEvent[endpointEvents.size()]);
        }

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

                        // Cancel ongoing signal if different from new signal
                        if(original != null && !original.equals(signal)) {
                            original.cancel();
                        }
                        
                        // Execute new signal if there is no ongoing equivalent signal
                        if(original == null || !original.equals(signal)) {
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
        MgcpRequest request = null;
        
        // Process event (if eligible)
        if(originator instanceof MgcpSignal) {
            request = onEndpointEvent((MgcpSignal) originator, event);
        } else if (originator instanceof MgcpConnection) {
            request = onConnectionEvent((MgcpConnection) originator, event);
        }
        
        if (request != null) {
            // Send notification to call agent
            // TODO hard-coded port in FROM field
            InetSocketAddress from = new InetSocketAddress(this.endpointId.getDomainName(), 2427);
            InetSocketAddress to = new InetSocketAddress(this.notifiedEntity.getDomain(), this.notifiedEntity.getPort());
            notify(this, from, to, request, MessageDirection.OUTGOING);
        }
    }
    
    private MgcpRequest onEndpointEvent(MgcpSignal signal, MgcpEvent event) {
        // Verify if endpoint is listening for such event
        final String composedName = event.getPackage() + "/" + event.getSymbol();
        if (isListening(composedName)) {
            // Unregister from current event
            this.signals.remove(signal.getName());

            // Build Notification
            MgcpRequest notify = new MgcpRequest();
            notify.setRequestType(MgcpRequestType.NTFY);
            notify.setTransactionId(0);
            notify.setEndpointId(this.endpointId.toString());

            NotifiedEntity entity = signal.getNotifiedEntity();
            if (entity != null) {
                notify.addParameter(MgcpParameterType.NOTIFIED_ENTITY, this.notifiedEntity.toString());
            }
            notify.addParameter(MgcpParameterType.OBSERVED_EVENT, event.toString());
            notify.addParameter(MgcpParameterType.REQUEST_ID, Integer.toString(signal.getRequestId(), 16));
            return notify;
        }
        return null;
    }
    
    private MgcpRequest onConnectionEvent(MgcpConnection connection, MgcpEvent event) {
        if(log.isDebugEnabled()) {
            log.debug(this.endpointId + " received MGCP event " + event.toString() + " from connection " + connection.getHexIdentifier());
        }
        
        // Verify if endpoint is listening for such event
        final String composedName = event.getPackage() + "/" + event.getSymbol();
        final int connectionId = connection.getIdentifier();
        
        MgcpRequestedEvent requestedEvent = isListening(connectionId, composedName);
        if(requestedEvent != null) {
            boolean removed = this.requestedConnectionEvents.remove(connectionId, requestedEvent);
            if(removed) {
              // Build Notification
              MgcpRequest notify = new MgcpRequest();
              notify.setRequestType(MgcpRequestType.NTFY);
              notify.setTransactionId(0);
              notify.setEndpointId(this.endpointId.toString());
      
              notify.addParameter(MgcpParameterType.OBSERVED_EVENT, event.toString());
              notify.addParameter(MgcpParameterType.REQUEST_ID, String.valueOf(requestedEvent.getRequestId()));
              return notify;
            }
        } else {
            // Special case: Connection timeout after allowed lifetime.
            if(event instanceof RtpTimeoutEvent) {
                try {
                    deleteConnection(connection.getCallIdentifier(), connectionId);
                } catch (MgcpConnectionNotFoundException | MgcpCallNotFoundException e) {
                    log.warn("Could not delete timed out connection " + connection.getHexIdentifier(), e);
                }
            }
        }
        return null;
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
        for (MgcpRequestedEvent evt : this.requestedEndpointEvents) {
            if (evt.getQualifiedName().equalsIgnoreCase(event)) {
                return true;
            }
        }
        return false;
    }

    private MgcpRequestedEvent isListening(int connectionId, String event) {
        Collection<MgcpRequestedEvent> events = this.requestedConnectionEvents.get(connectionId);
        for (MgcpRequestedEvent evt : events) {
            if (evt.getQualifiedName().equalsIgnoreCase(event)) {
                return evt;
            }
        }
        return null;
    }

}
