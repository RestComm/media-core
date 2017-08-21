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
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Unregisters all connections bound to a certain Call-ID.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>CALLBACK</li>
 * <li>CALL_ID</li>
 * <li>CONNECTION_ID</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>CONNECTION_COUNT</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterConnectionAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(UnregisterConnectionAction.class);

    static final UnregisterConnectionAction INSTANCE = new UnregisterConnectionAction();

    UnregisterConnectionAction() {
        super();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event, MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpEndpointContext globalContext = stateMachine.getContext();
        EndpointIdentifier endpointId = globalContext.getEndpointId();
        Map<Integer, MgcpConnection> connections = globalContext.getConnections();
        Integer callId = context.get(MgcpEndpointParameter.CALL_ID, Integer.class);
        Integer connectionId = context.get(MgcpEndpointParameter.CONNECTION_ID, Integer.class);
        FutureCallback<MgcpConnection> callback = context.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);

        // Find connection by connection-id
        MgcpConnection connection = connections.get(connectionId);

        if (connection == null) {
            String connectionIdHex = Integer.toHexString(connectionId).toUpperCase();
            Throwable t = new MgcpConnectionNotFoundException("Endpoint " + endpointId + " could not find connection " + connectionIdHex);
            callback.onFailure(t);
        } else if (connection.getCallIdentifier() != callId) {
            String connectionIdHex = Integer.toHexString(connectionId).toUpperCase();
            String callIdHex = Integer.toHexString(callId).toUpperCase();
            Throwable t = new MgcpCallNotFoundException("Endpoint " + endpointId + " could not find connection " + connectionIdHex + " in call " + callIdHex);
            callback.onFailure(t);
        } else {
            // unregister connection
            connections.remove(connectionId);

            // stop observing connection
            MgcpEventObserver eventObserver = context.get(MgcpEndpointParameter.EVENT_OBSERVER, MgcpEventObserver.class);
            connection.forget(eventObserver);

            if (log.isDebugEnabled()) {
                String connectionIdHex = Integer.toHexString(connectionId).toUpperCase();
                String callIdHex = Integer.toHexString(callId).toUpperCase();
                int connectionCount = connections.size();
                log.debug("Endpoint " + endpointId + " unregistered connection " + connectionIdHex + " from call " + callIdHex + ". Connection count: " + connectionCount);
            }
            
            // set output parameters
            context.set(MgcpEndpointParameter.CONNECTION_COUNT, connections.size());

            // warn FSM that connection was unregistered
            stateMachine.fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, context);

            // warn callback that operation completed successfully
            callback.onSuccess(connection);
        }
    }

}
