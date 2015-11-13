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

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains a list of active MGCP calls.
 * 
 * @author yulian oifa
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CallManager {

    private final ConcurrentHashMap<Integer, MgcpCall> calls;

    public CallManager() {
        this.calls = new ConcurrentHashMap<Integer, MgcpCall>();
    }

    public MgcpCall createCall(int id) {
        MgcpCall call = new MgcpCall(this, id);
        MgcpCall oldValue = this.calls.putIfAbsent(id, call);

        if (oldValue == null) {
            return call;
        } else {
            throw new IllegalArgumentException("Call with ID=" + id + " already exists");
        }
    }

    public MgcpCall getCall(int id) {
        return this.calls.get(id);
    }

    /**
     * Terminates specified call.
     * 
     * @param call the call to be terminated
     */
    protected void terminate(int callId) {
        calls.remove(callId);
    }

}
