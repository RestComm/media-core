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

package org.restcomm.media.control.mgcp.connection;

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.control.mgcp.exception.UnsupportedMgcpEventException;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.pkg.MgcpEventSubject;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.spi.ConnectionMode;

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
public interface MgcpConnection extends MgcpEventSubject {

    /**
     * Gets the connection identifier.
     * 
     * @return The connection identifier, in base 10
     */
    int getIdentifier();

    /**
     * Gets the connection identifier.
     * 
     * @return The connection identifier, in base 16.
     */
    String getHexIdentifier();

    /**
     * Gets the call identifier.
     * 
     * @return The call identifier, in base 10
     */
    int getCallIdentifier();

    /**
     * Gets the call identifier.
     * 
     * @return The connection identifier, in base 16
     */
    String getCallIdentifierHex();

    /**
     * Gets whether the connection is local or remote.
     * 
     * @return <code>true</code> if connection is local; <code>false</code> if it is remote.
     */
    boolean isLocal();

    /**
     * Gets the current mode of the connection.
     * 
     * @return The connection mode
     */
    ConnectionMode getMode();

    /**
     * Sets the mode of the connection.
     * 
     * @param mode The new mode of the connection
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void updateMode(ConnectionMode mode, FutureCallback<String> callback);

    /**
     * The connection allocates resources and becomes half-open, sending an SDP offer to the remote peer.
     * <p>
     * The connection must then wait for the remote peer to reply with MDCX request containing an SDP answer or for a DLCX
     * request that terminates the connection. description.
     * </p>
     * 
     * @param options The options that configure the connection.
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void halfOpen(LocalConnectionOptions options, FutureCallback<String> callback);

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
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void open(String sdp, FutureCallback<String> callback);
    
    /**
     * Re-negotiates an open connection.
     * 
     * @param sdp The new remote session description.
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void negotiate(String sdp, FutureCallback<String> callback);

    /**
     * Closes the connection.
     * 
     * @param callback Invoked when operation completes or fails.
     */
    void close(FutureCallback<Void> callback);

    /**
     * Requests the connection to send notifications about a certain event.
     * 
     * @param event The event to liste to.
     * @throws UnsupportedMgcpEventException If the connection does not support the event.
     */
    void listen(MgcpRequestedEvent event) throws UnsupportedMgcpEventException;

    /**
     * Gets the in-band audio component of the connection.
     * 
     * @return The in-band media component
     */
    AudioComponent getAudioComponent();

    /**
     * Gets the out-of-band audio component of the connection.
     * 
     * @return The out-of-band media component
     */
    OOBComponent getOutOfBandComponent();

}
