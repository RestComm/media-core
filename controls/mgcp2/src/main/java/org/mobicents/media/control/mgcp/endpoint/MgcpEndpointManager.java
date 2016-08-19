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

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mobicents.media.control.mgcp.endpoint.provider.MgcpEndpointProvider;
import org.mobicents.media.control.mgcp.exception.MgcpEndpointNotFoundException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpMessageSubject;

/**
 * Manages the collection of endpoints registered system-wide.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointManager implements MgcpEndpointObserver, MgcpMessageObserver, MgcpMessageSubject {

    // Endpoint Management
    private final ConcurrentHashMap<String, MgcpEndpointProvider<?>> providers;
    private final ConcurrentHashMap<String, MgcpEndpoint> endpoints;

    // Message Passing
    private final Collection<MgcpMessageObserver> observers;

    public MgcpEndpointManager() {
        // Endpoint Management
        this.endpoints = new ConcurrentHashMap<>(100);
        this.providers = new ConcurrentHashMap<>(5);

        // Message Passing
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void installProvider(MgcpEndpointProvider<?> provider) throws IllegalArgumentException {
        MgcpEndpointProvider<?> old = this.providers.putIfAbsent(provider.getNamespace(), provider);

        if (old != null) {
            throw new IllegalArgumentException("Provider for namespace " + provider.getNamespace() + "already exists.");
        }
    }

    public void uninstallProvider(String namespace) {
        this.providers.remove(namespace);
    }

    public boolean supportsNamespace(String namespace) {
        return this.providers.containsKey(namespace);
    }

    /**
     * Registers a new endpoint.
     * 
     * @param endpoint The name space of the endpoint which indicates what kind of endpoint is generated.
     */
    public MgcpEndpoint registerEndpoint(String namespace) throws UnrecognizedMgcpNamespaceException {
        // Get correct endpoint provider
        MgcpEndpointProvider<?> provider = this.providers.get(namespace);
        if (provider == null) {
            throw new UnrecognizedMgcpNamespaceException("Namespace " + namespace + " is unrecognized");
        }

        // Create the endpoint and register it
        MgcpEndpoint endpoint = provider.provide();
        endpoint.observe((MgcpEndpointObserver) this);
        endpoint.observe((MgcpMessageObserver) this);
        this.endpoints.put(endpoint.getEndpointId().toString(), endpoint);
        return endpoint;
    }

    /**
     * Gets a registered endpoint.
     * 
     * @param endpointId The endpoint identifier.
     * @return The endpoint if registered; otherwise returns null
     */
    public MgcpEndpoint getEndpoint(String endpointId) {
        return this.endpoints.get(endpointId);
    }

    /**
     * Unregisters an active endpoint.
     * 
     * @param endpointId The ID of the endpoint to be unregistered
     * @throws MgcpEndpointNotFoundException If there is no registered endpoint with such ID
     */
    public void unregisterEndpoint(String endpointId) throws MgcpEndpointNotFoundException {
        MgcpEndpoint endpoint = this.endpoints.remove(endpointId);
        if (endpoint == null) {
            throw new MgcpEndpointNotFoundException("Endpoint " + endpointId + " not found");
        }
        endpoint.forget((MgcpMessageObserver) this);
        endpoint.forget((MgcpEndpointObserver) this);
    }

    @Override
    public void onMessage(MgcpMessage message, MessageDirection direction) {
        notify(this, message, direction);
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.observers.remove(observer);
    }

    @Override
    public void notify(Object originator, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = iterator.next();
            if (observer != originator) {
                observer.onMessage(message, direction);
            }
        }
    }

    @Override
    public void onEndpointStateChanged(MgcpEndpoint endpoint, MgcpEndpointState state) {
        if(MgcpEndpointState.INACTIVE.equals(state)) {
            this.endpoints.remove(endpoint.getEndpointId());
        }
    }

}
