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
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
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
    
    private static final Logger log = Logger.getLogger(MgcpEndpointManager.class);

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
        } else {
            if (log.isInfoEnabled()) {
                log.info("Installed MGCP Endpoint Provider for namespace " + provider.getNamespace());
            }
        }
    }

    public void uninstallProvider(String namespace) {
        MgcpEndpointProvider<?> provider = this.providers.remove(namespace);
        if(provider != null) {
            if (log.isInfoEnabled()) {
                log.info("Uninstalled MGCP Endpoint Provider for namespace " + provider.getNamespace());
            }
        }
    }

    public boolean supportsNamespace(String namespace) {
        return this.providers.containsKey(namespace);
    }

    /**
     * Registers a new endpoint.
     * 
     * @param endpoint The name space of the endpoint which indicates what kind of endpoint is generated.
     * @param domain The domain where the endpoint is to be registered.
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
        
        if (log.isDebugEnabled()) {
            log.debug("Registered endpoint " + endpoint.getEndpointId().toString() + ". Count: " + this.endpoints.size());
        }
        
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
        
        if (log.isDebugEnabled()) {
            log.debug("Unregistered endpoint " + endpoint.getEndpointId().toString() + ". Count: " + this.endpoints.size());
        }
    }

    @Override
    public void onMessage(InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        notify(this, from, to, message, direction);
    }

    @Override
    public void observe(MgcpMessageObserver observer) {
        this.observers.add(observer);
        if (log.isTraceEnabled()) {
            log.trace("Registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void forget(MgcpMessageObserver observer) {
        this.observers.remove(observer);
        if (log.isTraceEnabled()) {
            log.trace("Registered MgcpMessageObserver@" + observer.hashCode() + ". Count: " + this.observers.size());
        }
    }

    @Override
    public void notify(Object originator, InetSocketAddress from, InetSocketAddress to, MgcpMessage message, MessageDirection direction) {
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = iterator.next();
            if (observer != originator) {
                observer.onMessage(from, to, message, direction);
            }
        }
    }

    @Override
    public void onEndpointStateChanged(MgcpEndpoint endpoint, MgcpEndpointState state) {
        if (log.isTraceEnabled()) {
            log.debug("Endpoint " + endpoint.getEndpointId().toString() + " changed state to " + state.name());
        }
        
        if(MgcpEndpointState.INACTIVE.equals(state)) {
            final String endpointId = endpoint.getEndpointId().toString();
            try {
                unregisterEndpoint(endpointId);
            } catch (MgcpEndpointNotFoundException e) {
                log.warn("Could not unregister endpoint " + endpointId +": Not found.");
            }
        }
    }

}
