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
package org.mobicents.media.server.mgcp.controller;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MGCP call.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MgcpCall {

    private final CallManager callManager;

    private final int id;
    private final ConcurrentHashMap<String, MgcpEndpoint> endpoints;

    protected MgcpCall(CallManager callManager, int id) {
        this.callManager = callManager;
        this.id = id;
        this.endpoints = new ConcurrentHashMap<String, MgcpEndpoint>();
    }

    public int getId() {
        return id;
    }

    public MgcpEndpoint getMgcpEndpoint(String endpointName) {
        return this.endpoints.get(endpointName);
    }

    public void addEndpoint(MgcpEndpoint endpoint) {
        this.endpoints.putIfAbsent(endpoint.getName(), endpoint);
    }

    public MgcpEndpoint removeEndpoint(String endpointName) {
        MgcpEndpoint endpoint = this.endpoints.remove(endpointName);
        if (endpoint != null) {
            // Cascade delete connections from the endpoint
            endpoint.deleteAllConnections();

            // Terminate call if there are no endpoints left
            if (this.endpoints.isEmpty()) {
                this.callManager.terminate(this.id);
            }
        }
        return endpoint;
    }

    public void removeEndpoints() {
        Iterator<String> keys = this.endpoints.keySet().iterator();
        while(keys.hasNext()) {
            String endpointName = keys.next();
            removeEndpoint(endpointName);
        }
    }
    
    public int countEndpoints() {
        return this.endpoints.size();
    }

    @Override
    public String toString() {
        return "call-" + id;
    }
}
