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

package org.restcomm.media.control.mgcp.call;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * A Call is responsible to group Connections that belong to the same session.
 * <p>
 * The concept of Call has little semantic meaning in the MGCP protocol. However, it can be used to identify calls for reporting
 * and accounting purposes. It does not affect the handling of connections by the gateway.
 * </p>
 * <p>
 * Calls are identified by unique identifiers, independent of the underlying platforms or agents.<br>
 * Call identifiers are hexadecimal strings, which are created by the Call Agent. The maximum length of call identifiers is 32
 * characters.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCall {

    private static final Logger log = Logger.getLogger(MgcpCall.class);

    private final int callId;
    private final SetMultimap<String, Integer> entries;

    public MgcpCall(int callId) {
        this.callId = callId;
        this.entries = Multimaps.synchronizedSetMultimap(HashMultimap.<String, Integer> create(3, 3));
    }

    /**
     * Gets the call identifier.
     * 
     * @return The call identifier in base 10.
     */
    public int getCallId() {
        return callId;
    }

    /**
     * Gets the call identifier.
     * 
     * @return The call identifier in base 16.
     */
    public String getCallIdHex() {
        return Integer.toHexString(this.callId).toUpperCase();
    }

    /**
     * Gets the list of currently registered endpoints that own the connections in the Call.
     * 
     * @return An <b>unmodifiable</b> set of endpoint identifiers. If no endpoints are registered, an empty set is returned.
     */
    public Set<String> getEndpoints() {
        return Collections.unmodifiableSet(this.entries.keySet());
    }

    /**
     * Gets the list of currently registered connections in the Call.
     * 
     * @return An <b>unmodifiable</b> set of connection identifiers. If no connections are registered, an empty set is returned.
     */
    public Set<Integer> getConnections(String endpointId) {
        return Collections.unmodifiableSet(this.entries.get(endpointId));
    }

    /**
     * Registers a connection in the call.
     * 
     * @param endpointId The identifier of the endpoint that owns the connection.
     * @param connectionId The connection identifier.
     * @return Returns <code>true</code> if connection was successfully registered. Returns <code>false</code> otherwise.
     */
    public boolean addConnection(String endpointId, int connectionId) {
        boolean added = this.entries.put(endpointId, connectionId);
        if (added && log.isDebugEnabled()) {
            int left = this.entries.get(endpointId).size();
            log.debug("Call " + getCallIdHex() + " registered connection " + Integer.toHexString(connectionId) + " at endpoint " + endpointId + ". Connection count: " + left);
        }
        return added;
    }

    /**
     * Unregisters a connection from the call.
     * 
     * @param endpointId The identifier of the endpoint that owns the connection.
     * @param connectionId The connection identifier.
     * @return Returns <code>true</code> if connection was removed successfully. Returns <code>false</code> otherwise.
     */
    public boolean removeConnection(String endpointId, int connectionId) {
        boolean removed = this.entries.remove(endpointId, connectionId);
        if (removed && log.isDebugEnabled()) {
            int left = this.entries.get(endpointId).size();
            log.debug("Call " + getCallIdHex() + " unregistered connection " + Integer.toHexString(connectionId) + " from endpoint " + endpointId + ". Connection count: " + left);
        }
        return removed;
    }

    /**
     * Unregisters all connections that belong to an endpoint.
     * 
     * @param endpointId The identifier of the endpoint that owns the connections.
     * @return A set containing all unregistered connection identifiers. Returns an empty set if no connections exist.
     */
    public Set<Integer> removeConnections(String endpointId) {
        Set<Integer> removed = this.entries.removeAll(endpointId);
        if (!removed.isEmpty() && log.isDebugEnabled()) {
            log.debug("Call " + getCallIdHex() + " unregistered connections " + Arrays.toString(convertToHex(removed)) + " from endpoint " + endpointId);
        }
        return removed;
    }

    private String[] convertToHex(Collection<Integer> identifiers) {
        String[] converted = new String[identifiers.size()];
        int index = 0;
        for (int id : identifiers) {
            converted[index] = Integer.toHexString(id);
            index++;
        }
        return converted;
    }

}
