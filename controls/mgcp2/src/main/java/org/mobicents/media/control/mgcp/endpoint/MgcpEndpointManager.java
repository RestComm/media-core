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

import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.media.control.mgcp.endpoint.provider.MgcpEndpointProvider;
import org.mobicents.media.control.mgcp.exception.MgcpEndpointNotFoundException;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;

/**
 * Manages the collection of endpoints registered system-wide.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointManager {

    private final ConcurrentHashMap<String, MgcpEndpoint> endpoints;
    private final ConcurrentHashMap<String, MgcpEndpointProvider<?>> providers;

    public MgcpEndpointManager() {
        this.endpoints = new ConcurrentHashMap<>(100);
        this.providers = new ConcurrentHashMap<>(5);
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
        this.endpoints.put(endpoint.getEndpointId(), endpoint);
        return endpoint;
    }

    /**
     * Gets a registered endpoint.
     * 
     * @param endpointId The endpoint identifier.
     * @return The endpoint if registered; otherwise returns null
     */
    public MgcpEndpoint getEndpoint(String endpointId) {
        String localName = resolveEndpointId(endpointId);
        return this.endpoints.get(localName);
    }

    /**
     * Unregisters an active endpoint.
     * 
     * @param endpointId The ID of the endpoint to be unregistered
     * @throws MgcpEndpointNotFoundException If there is no registered endpoint with such ID
     */
    public void unregisterEndpoint(String endpointId) throws MgcpEndpointNotFoundException {
        String localName = resolveEndpointId(endpointId);
        MgcpEndpoint endpoint = this.endpoints.remove(localName);
        if (endpoint == null) {
            throw new MgcpEndpointNotFoundException("Endpoint " + endpointId + " not found");
        }
    }

    // FIXME Right now only deals with localName. Should take domain into account in the future.
    private String resolveEndpointId(String endpointId) {
        String result = endpointId;
        int indexOfSeparator = endpointId.indexOf("@");
        if(indexOfSeparator > -1) {
            result = endpointId.substring(0, indexOfSeparator);
        }
        return result;
    } 

}
