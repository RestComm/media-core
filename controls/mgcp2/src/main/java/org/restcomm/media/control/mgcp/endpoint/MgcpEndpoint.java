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

package org.restcomm.media.control.mgcp.endpoint;

import org.restcomm.media.control.mgcp.command.rqnt.NotificationRequest;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.message.MgcpMessageSubject;
import org.restcomm.media.control.mgcp.pkg.MgcpEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;

import com.google.common.util.concurrent.FutureCallback;

/**
 * An Endpoint is a logical representation of a physical entity, such as an analog phone or a channel in a trunk.
 * <p>
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
 */
public interface MgcpEndpoint extends MgcpEndpointSubject, MgcpMessageSubject, MgcpEventObserver {

    /**
     * Gets the endpoint identifier
     *
     * @return The endpoint ID
     */
    EndpointIdentifier getEndpointId();

    /**
     * Gets a connection by identifier.
     *
     * @param callId       The call identifier.
     * @param connectionId The connection identifier.
     * @return The connection matching the criteria. Returns null if no such connection exists.
     */
    MgcpConnection getConnection(int callId, int connectionId);

    /**
     * Gets whether a connection is registered.
     *
     * @param callId       The call identifier.
     * @param connectionId The connection identifier.
     * @return <code>true</code> if connection is registered; <code>false</code> otherwise.
     */
    boolean isRegistered(int callId, int connectionId);

    /**
     * Registers a connection.
     *
     * @param connection The connection to be registered
     * @param callback   The callback that will be triggered once task completes.
     */
    void registerConnection(MgcpConnection connection, FutureCallback<Void> callback);

    /**
     * Unregisters a connection from the endpoint. <b>It is the caller's responsibility to close the connection</b>
     *
     * @param callId       The ID of the call where the connection is stored.
     * @param connectionId The connection ID
     * @param callback     Callback that holds unregistered connection. May hold MgcpCallNotFoundException or
     *                     MgcpConnectionNotFoundException if operation is not successful.
     */
    void unregisterConnection(int callId, int connectionId, FutureCallback<MgcpConnection> callback);

    /**
     * Unregisters all currently active connections. <b>It is the caller's responsibility to close the connection</b>
     *
     * @param callback The callback that holds a list of unregistered connections. The list may be empty if the endpoint does
     *                 not contain any connections.
     */
    void unregisterConnections(FutureCallback<MgcpConnection[]> callback);

    /**
     * Deletes all currently active connections within a specific call. <b>It is the caller's responsibility to close the
     * connection</b>
     *
     * @param callId   the call identifier
     * @param callback The callback that holds a list of unregistered connections. May hold MgcpCallNotFoundException if there
     *                 are no connections bound to the call-id in the endpoint.
     */
    void unregisterConnections(int callId, FutureCallback<MgcpConnection[]> callback);

    /**
     * Requests a notification to be fired when an event happens in the endpoint.
     *
     * @param request  The notification request.
     * @param callback The callback that is notified about completion of the notification request.
     */
    void requestNotification(NotificationRequest request, FutureCallback<Void> callback);

    /**
     * Raises an event triggered by a quarantined signal.
     * <p>
     * <b>NOTE:</b> To be used in special situations only, such as EndSignal.
     *
     * @param requestId The request identifier that belongs to the signal.
     * @param signal    The name of the signal.
     * @param callback  The callback that is notified about completion of the task.
     */
    void endSignal(String requestId, String signal, FutureCallback<MgcpEvent> callback);

    /**
     * Gets the media group that holds media components of the endpoint.
     *
     * @return The media group.
     */
    MediaGroup getMediaGroup();
}
