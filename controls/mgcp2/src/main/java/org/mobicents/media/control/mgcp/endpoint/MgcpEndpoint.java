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

import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;

/**
 * An Endpoint is a logical representation of a physical entity, such as an analog phone or a channel in a trunk.
 * 
 * <p>
 * Endpoints are sources or sinks of data and can be physical or virtual. Physical endpoint creation requires hardware
 * installation while software is sufficient for creating a virtual Endpoint.
 * </p>
 * <p>
 * An interface on a gateway that terminates a trunk connected to a PSTN switch is an example of a physical Endpoint. An audio
 * source in an audio-content server is an example of a virtual Endpoint.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpEndpoint {

    /**
     * Gets the endpoint identifier
     * 
     * @return The endpoint ID
     */
    String getEndpointId();

    /**
     * Gets a connection by identifier.
     * 
     * @param callId The call identifier.
     * @param connectionId The connection identifier.
     * @return The connection matching the criteria. Returns null if no such connection exists.
     */
    MgcpConnection getConnection(int callId, int connectionId);

    /**
     * Registers a connection.
     * 
     * @param callId The identifier of the call where the connection belongs to.
     * @param connection The connection to be registered.
     * @throws MgcpConnectionException When connection is duplicate.
     */
    void addConnection(int callId, MgcpConnection connection) throws MgcpConnectionException;

    /**
     * Deletes an active connection.
     * 
     * @param callId The ID of the call where the connection is stored.
     * @param connectionId The connection ID
     * @throws MgcpCallNotFoundException When call with such ID cannot be found.
     * @throws MgcpConnectionNotFound When call does not contain connection with such ID.
     */
    void deleteConnection(int callId, int connectionId) throws MgcpCallNotFoundException, MgcpConnectionNotFound;

    /**
     * Deletes all currently active connections.
     */
    void deleteConnections();

    /**
     * Deletes all currently active connections within a specific call.
     * 
     * @param callId the call identifier
     * @throws MgcpCallNotFoundException When call with such ID cannot be found.
     */
    void deleteConnections(int callId) throws MgcpCallNotFoundException;

}
