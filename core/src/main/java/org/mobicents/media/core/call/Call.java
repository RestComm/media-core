/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.core.call;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.media.core.endpoints.EndpointFactory;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;

/**
 * Entity that manages a group of endpoints and connections that are created by the call agent during a call.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Call {

    private final int id;
    private final ConcurrentHashMap<String, Endpoint> endpoints;

    public Call(int id) {
        this.id = id;
        this.endpoints = new ConcurrentHashMap<String, Endpoint>();
    }

    public int getId() {
        return id;
    }

    public boolean hasEndpoints() {
        return this.endpoints.isEmpty();
    }

    public int countEndpoints() {
        return this.endpoints.size();
    }

    public Endpoint getEndpoint(String endpointName) {
        return this.endpoints.get(endpointName);
    }
    
    public Endpoint createEndpoint(EndpointType type) {
        Endpoint endpoint = EndpointFactory.createEndpoint(type);
        addEndpoint(endpoint);
        return endpoint;
    }

    public void addEndpoint(Endpoint endpoint) {
        this.endpoints.putIfAbsent(endpoint.getLocalName(), endpoint);
    }

    public Endpoint deleteEndpoint(String endpointName) {
        Endpoint endpoint = this.endpoints.remove(endpointName);
        if (endpoint != null) {
            endpoint.deleteConnections();
        }
        return endpoint;
    }

    public void deleteEndpoints() {
        Iterator<String> iterator = this.endpoints.keySet().iterator();
        while (iterator.hasNext()) {
            deleteEndpoint(iterator.next());
        }
    }

    @Override
    public String toString() {
        return "call-" + this.id;
    }
}
