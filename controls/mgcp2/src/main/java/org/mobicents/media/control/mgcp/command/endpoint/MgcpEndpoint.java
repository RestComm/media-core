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
     * Creates a new connection.
     * 
     * @return The newly created connection;
     */
    MgcpConnection createConnection();

    /**
     * Retrieves a connection associated with the endpoint.
     * 
     * @param id The connection ID
     * @return Returns the connection if it exists; otherwise returns null
     */
    MgcpConnection getConnection(int id);

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
     * Activates the endpoint.
     * 
     * @throws IllegalStateException If endpoint is already active
     */
    void activate() throws IllegalStateException;

    /**
     * Deactivates the endpoint.
     * 
     * @throws IllegalStateException If endpoint is already inactive
     */
    void deactivate();

}
