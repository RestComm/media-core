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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class MgcpEndpointContext {

    // Media Components
    protected final MediaGroup mediaGroup;

    // Endpoint Properties
    private final EndpointIdentifier endpointId;
    private final ConcurrentHashMap<Integer, MgcpConnection> connections;

    // Events and Signals
    private final NotificationCenter notificationCenter;
    // TODO add support for MGCP Connection Events
    private final Multimap<Integer, MgcpRequestedEvent> requestedConnectionEvents;

    // Observers
    private final Set<MgcpEndpointObserver> endpointObservers;
    private final Set<MgcpMessageObserver> messageObservers;

    protected MgcpEndpointContext(EndpointIdentifier endpointId, MediaGroup mediaGroup, NotificationCenter notificationCenter) {
        // Endpoint Properties
        this.endpointId = endpointId;
        this.connections = new ConcurrentHashMap<>(5);

        // Media Components
        this.mediaGroup = mediaGroup;

        // Events and Signals
        this.notificationCenter = notificationCenter;
        this.requestedConnectionEvents = Multimaps.synchronizedSetMultimap(HashMultimap.<Integer, MgcpRequestedEvent>create());

        // Observers
        this.endpointObservers = Sets.newConcurrentHashSet();
        this.messageObservers = Sets.newConcurrentHashSet();
    }

    public NotificationCenter getNotificationCenter() {
        return notificationCenter;
    }

    public MediaGroup getMediaGroup() {
        return mediaGroup;
    }

    public EndpointIdentifier getEndpointId() {
        return endpointId;
    }

    boolean hasConnections() {
        return !this.connections.isEmpty();
    }

    public ConcurrentHashMap<Integer, MgcpConnection> getConnections() {
        return connections;
    }

    public Multimap<Integer, MgcpRequestedEvent> getRequestedConnectionEvents() {
        return requestedConnectionEvents;
    }

    public Set<MgcpEndpointObserver> getEndpointObservers() {
        return endpointObservers;
    }

    public Set<MgcpMessageObserver> getMessageObservers() {
        return messageObservers;
    }

}
