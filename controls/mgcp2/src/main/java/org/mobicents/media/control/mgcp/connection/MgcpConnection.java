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

/**
 * Connections are created on each endpoint that will be involved in the call.
 * <p>
 * Each connection will be designated locally by an endpoint unique connection identifier, and will be characterized by
 * connection attributes.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 * @see <a href=""https://tools.ietf.org/html/rfc3435#section-2.1.3>RFC3435 - Section 2.1.3</a>
 */
public interface MgcpConnection {

    /**
     * Gets the connection identifier
     * 
     * @return The connection identifier
     */
    int getIdentifier();

    /**
     * Gets the connection identifier in hexadecimal base.
     * 
     * @return The connection identifier
     */
    String getHexIdentifier();

    /**
     * Gets whether the connection is local or remote.
     * 
     * @return <code>true</code> if connection is local; <code>false</code> if it is remote.
     */
    boolean isLocal();

    /**
     * Gets the current state of the connection.
     * 
     * @return The state of the connection
     */
    MgcpConnectionState getState();

    /**
     * Gets the current mode of the connection.
     * 
     * @return The connection mode
     */
    MgcpConnectionMode getMode();

    /**
     * Sets the mode of the connection.
     * 
     * @param mode The new mode of the connection
     * 
     * @throws IllegalStateException Cannot update mode of closed connections
     */
    void setMode(MgcpConnectionMode mode) throws IllegalStateException;

    /**
     * The connection allocates resources and becomes half-open, sending an SDP offer to the remote peer.
     * <p>
     * The connection must then wait for the remote peer to reply with MDCX request containing an SDP answer or for a DLCX
     * request that terminates the connection. description.
     * </p>
     * 
     * @return The SDP offer.
     * 
     * @throws IllegalStateException If connection state is not closed.
     */
    String halfOpen() throws IllegalStateException;

    /**
     * Moves the connection to an open state.
     * 
     * <p>
     * If the call is inbound, then the remote peer will provide an SDP offer and the connection will allocate resources and
     * provide and SDP answer right away.
     * </p>
     * <p>
     * If the call is outbound, the connection MUST move from an half-open state. In this case, the remote peer provides the
     * MGCP answer and the connection can be established between both peers.
     * </p>
     * 
     * @param sdp The SDP description of the remote peer.
     * @return The SDP answer if the call is inbound; <code>null</code> if call is outbound.
     * 
     * @throws IllegalStateException If connection state is not closed nor half-open.
     */
    String open(String sdp) throws IllegalStateException;

    /**
     * Closes the connection.
     * 
     * @throws IllegalStateException If connection state is not half-open nor open.
     */
    void close() throws IllegalStateException;

}
