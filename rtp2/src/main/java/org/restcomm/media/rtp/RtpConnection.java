/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.rtp;

import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Connection established between the Media Server and a remote peer from where RTP flows.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface RtpConnection {

    /**
     * Half opens the connection.
     * 
     * A media session is allocated and the local session description is generated.
     * 
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void halfOpen(FutureCallback<String> callback);

    /**
     * Fully opens the connection and connects it to remote peer.
     * 
     * @param remoteDescription The remote session description.
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void open(String remoteDescription, FutureCallback<String> callback);

    /**
     * Re-negotiates an open connection.
     * 
     * @param remoteDescription The remote session description
     * @param callback Invoked when operation completes or fails. Holds the local session description.
     */
    void modify(String remoteDescription, FutureCallback<String> callback);

    /**
     * Updates the connection mode.
     * 
     * @param mode The new connection mode.
     * @param callback Invoked when operation completes or fails.
     */
    void updateMode(ConnectionMode mode, FutureCallback<Void> callback);

    /**
     * Closes the connection.
     * 
     * @param callback Invoked when operation completes or fails.
     */
    void close(FutureCallback<Void> callback);

}
