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

package org.restcomm.media.control.mgcp.endpoint;

import java.util.Map;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.exception.DuplicateMgcpConnectionException;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RegisterConnectionAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(RegisterConnectionAction.class);

    @Override
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpEndpointContext globalContext = stateMachine.getContext();
        Map<Integer, MgcpConnection> connections = globalContext.getConnections();
        MgcpConnection connection = context.get(MgcpEndpointParameter.REGISTERED_CONNECTION, MgcpConnection.class);
        FutureCallback<?> callback = context.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);

        int connectionId = connection.getIdentifier();
        if (connections.containsKey(connectionId)) {
            // Register Connection
            connections.put(connectionId, connection);

            // MGCP Endpoint must observe events coming from the connection
            MgcpEventObserver eventObserver = context.get(MgcpEndpointParameter.EVENT_OBSERVER, MgcpEventObserver.class);
            if (!connection.isLocal()) {
                connection.observe(eventObserver);
            }

            if (log.isDebugEnabled()) {
                String connectionIdHex = connection.getHexIdentifier();
                String endpointId = globalContext.getEndpointId().toString();
                int connectionCount = connections.size();

                log.debug("Registered connection " + connectionIdHex + " in endpoint " + endpointId + ". Count: " + connectionCount);
            }

            // Notify callback that operation was successful
            callback.onSuccess(null);
        } else {
            // Connection with similar identifier is already register.
            // Abort operation and notify callback.
            String msg = "MGCP Endpoint " + globalContext.getEndpointId().toString() + " already contains connection " + connection.getHexIdentifier();
            Throwable t = new DuplicateMgcpConnectionException(connectionId, msg);
            callback.onFailure(t);
        }
    }

}
