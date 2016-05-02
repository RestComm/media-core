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

package org.mobicents.media.control.mgcp.command.endpoint;

import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionMode;
import org.mobicents.media.control.mgcp.message.LocalConnectionOptions;

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
     * Creates a new connection.
     * 
     * @param callId The the call identifies which indicates to which session the connection belongs to.
     * @param local Whether connection is local or remote
     */
    MgcpConnection createConnection(int callId, boolean local);

    /**
     * Modifies an existing connection.
     * 
     * @param callId The identifier of the call which the connection belongs to.
     * @param connectionId The connection identifier
     * @param notifiedEntity (optional) The new notified entity for the endpoint.
     * @param options (optional) New options for the connection.
     * @param mode (optional) The new connection mode.
     */
    void modifyConnection(int callId, int connectionId, String notifiedEntity, LocalConnectionOptions options,
            MgcpConnectionMode mode);

    /**
     * Deletes an active connection.
     * 
     * @param id The connection ID
     */
    void deleteConnection(int id);

    /**
     * Deletes all currently active connections.
     */
    void deleteConnections();

    /**
     * Deletes all currently active connections within a specific call.
     * 
     * @param callId the call identifier
     */
    void deleteConnections(int callId);

}
