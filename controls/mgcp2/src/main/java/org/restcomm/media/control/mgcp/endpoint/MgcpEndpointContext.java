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

package org.restcomm.media.control.mgcp.endpoint;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpSignal;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class MgcpEndpointContext {

    static final MgcpRequestedEvent[] EMPTY_ENDPOINT_EVENTS = new MgcpRequestedEvent[0];

    // Media Components
    protected final MediaGroup mediaGroup;

    // Endpoint Properties
    private final EndpointIdentifier endpointId;
    private final ConcurrentHashMap<Integer, MgcpConnection> connections;

    // Events and Signals
    private NotifiedEntity notifiedEntity;
    private ConcurrentHashMap<String, MgcpSignal> signals;
    // TODO requestedEndpointEvents needs to be synchronized!
    private MgcpRequestedEvent[] requestedEndpointEvents;
    private final Multimap<Integer, MgcpRequestedEvent> requestedConnectionEvents;

    // Observers
    private final Set<MgcpEndpointObserver> endpointObservers;
    private final Set<MgcpMessageObserver> messageObservers;

    MgcpEndpointContext(EndpointIdentifier endpointId, MgcpConnectionProvider connectionProvider, MediaGroup mediaGroup) {
        // Endpoint Properties
        this.endpointId = endpointId;
        this.connections = new ConcurrentHashMap<>(5);

        // Media Components
        this.mediaGroup = mediaGroup;

        // Events and Signals
        this.notifiedEntity = new NotifiedEntity();
        this.signals = new ConcurrentHashMap<>(5);
        this.requestedEndpointEvents = MgcpEndpointContext.EMPTY_ENDPOINT_EVENTS;
        this.requestedConnectionEvents = Multimaps.synchronizedSetMultimap(HashMultimap.<Integer, MgcpRequestedEvent> create());

        // Observers
        this.endpointObservers = Sets.newConcurrentHashSet();
        this.messageObservers = Sets.newConcurrentHashSet();
    }

    protected NotifiedEntity getNotifiedEntity() {
        return notifiedEntity;
    }

    protected void setNotifiedEntity(NotifiedEntity notifiedEntity) {
        this.notifiedEntity = notifiedEntity;
    }

    protected ConcurrentHashMap<String, MgcpSignal> getSignals() {
        return signals;
    }

    protected void setSignals(ConcurrentHashMap<String, MgcpSignal> signals) {
        this.signals = signals;
    }

    protected MgcpRequestedEvent[] getRequestedEndpointEvents() {
        return requestedEndpointEvents;
    }

    protected void setRequestedEndpointEvents(MgcpRequestedEvent[] requestedEndpointEvents) {
        this.requestedEndpointEvents = requestedEndpointEvents;
    }

    protected MediaGroup getMediaGroup() {
        return mediaGroup;
    }

    protected EndpointIdentifier getEndpointId() {
        return endpointId;
    }

    public boolean hasConnections() {
        return !this.connections.isEmpty();
    }

    protected ConcurrentHashMap<Integer, MgcpConnection> getConnections() {
        return connections;
    }

    protected Multimap<Integer, MgcpRequestedEvent> getRequestedConnectionEvents() {
        return requestedConnectionEvents;
    }

    protected Set<MgcpEndpointObserver> getEndpointObservers() {
        return endpointObservers;
    }

    protected Set<MgcpMessageObserver> getMessageObservers() {
        return messageObservers;
    }

}
