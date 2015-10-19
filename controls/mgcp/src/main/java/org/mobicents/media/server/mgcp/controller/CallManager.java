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

import org.mobicents.media.server.concurrent.ConcurrentMap;

/**
 * Maintains MGCP calls.
 * 
 * @author yulian oifa
 */
public class CallManager {

    private final ConcurrentMap<MgcpCall> calls;

    public CallManager() {
        this.calls = new ConcurrentMap<MgcpCall>();
    }

    /**
     * Creates and registers a new call.
     * 
     * @param id The identifier of the call.
     * @return Returns the newly created call. Returns null if a call with same identifier already exists.
     */
    public MgcpCall createCall(int id) {
        if (calls.containsKey(id)) {
            return null;
        }
        return this.calls.putIfAbsent(id, new MgcpCall(this, id));
    }

    /**
     * Retrieves a call by identifier.
     * 
     * @param id The identifier of the call.
     * @return The call with corresponding identifier. Return null if no call matches the identifier.
     */
    public MgcpCall getCall(int id) {
        return this.calls.put(id, new MgcpCall(this, id));
    }

    /**
     * Terminates specified call.
     * 
     * @param call the call to be terminated
     */
    protected void terminate(MgcpCall call) {
        calls.remove(call.id);
    }

    /**
     * Counts the number of existing calls.
     * 
     * @return The number of calls.
     */
    public int countCalls() {
        return this.calls.size();
    }

}
