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

package org.mobicents.media.control.mgcp.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCall {

    private static final Logger log = Logger.getLogger(MgcpCall.class);

    private final int id;
    private ConcurrentHashMap<Integer, MgcpConnection> connections;

    public MgcpCall(int id) {
        this.id = id;
        this.connections = new ConcurrentHashMap<>(6);
    }

    /**
     * Gets the call identifier.
     * 
     * @return The call ID
     */
    public int getId() {
        return id;
    }
    
    public boolean hasConnections() {
        return !this.connections.isEmpty();
    }
    
    public int countConnections() {
        return this.connections.size();
    }

    /**
     * Gets a registered connection.
     * 
     * @param connectionId The connection ID
     * @return The connection with matching ID or null if no connection matches the criteria.
     */
    public MgcpConnection getConnection(int connectionId) {
        return this.connections.get(connectionId);
    }

    /**
     * Registers a connection in the call.
     * 
     * @param connection The connection to be registered
     * @throws IllegalArgumentException If a connection with similar identifier is already registered in the call
     */
    public void addConnection(MgcpConnection connection) throws IllegalArgumentException {
        int connectionId = connection.getIdentifier();
        MgcpConnection oldConnection = this.connections.putIfAbsent(connectionId, connection);

        // Verify connection is not a duplicate
        if (oldConnection != null) {
            throw new IllegalArgumentException("Conection " + connection.getHexIdentifier() + " is already registered in call " + this.id);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Registered connection " + connection.getHexIdentifier() + " to call " + this.id + ". Count: " + this.connections.size());
            }
        }
    }

    /**
     * Unregisters a connection from the call.
     * 
     * @param connectionId The connection identifier.
     * @return The unregistered conneciton.
     */
    public MgcpConnection removeConnection(int connectionId) {
        MgcpConnection connection = this.connections.remove(connectionId);
        if (log.isDebugEnabled() && connection != null) {
            log.debug("Unregistered connection " + connection.getHexIdentifier() + " from call " + this.id + ". Count: " + this.connections.size());
        }
        // TODO alert listener that call has ended if connections == 0
        return connection;
    }

    /**
     * Unregisters all connections from the call.
     * 
     * @return The list of unregistered connections.
     */
    public List<MgcpConnection> removeConnections() {
        ArrayList<MgcpConnection> values = new ArrayList<>(this.connections.values());
        this.connections.clear();
        if (log.isDebugEnabled()) {
            log.debug("Unregistered "+ values.size() +" connections from call " + this.id + ". Count: " + this.connections.size());
        }
        return values;
    }

}
