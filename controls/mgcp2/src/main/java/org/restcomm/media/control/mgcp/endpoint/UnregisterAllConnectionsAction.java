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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.pkg.MgcpEventObserver;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Unregisters all connections bound to a certain Call-ID.
 * 
 * <p>
 * Input parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * <p>
 * Output parameters:
 * <ul>
 * <li>n/a</li>
 * </ul>
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterAllConnectionsAction
        extends AnonymousAction<MgcpEndpointFsm, MgcpEndpointState, MgcpEndpointEvent, MgcpEndpointTransitionContext>
        implements MgcpEndpointAction {

    private static final Logger log = Logger.getLogger(UnregisterAllConnectionsAction.class);

    @Override
    @SuppressWarnings("unchecked")
    public void execute(MgcpEndpointState from, MgcpEndpointState to, MgcpEndpointEvent event,
            MgcpEndpointTransitionContext context, MgcpEndpointFsm stateMachine) {
        MgcpEndpointContext globalContext = stateMachine.getContext();
        FutureCallback<MgcpConnection[]> callback = context.get(MgcpEndpointParameter.CALLBACK, FutureCallback.class);
        MgcpEventObserver observer = context.get(MgcpEndpointParameter.EVENT_OBSERVER, MgcpEventObserver.class);
        
        // Unregister all connections bound to a call
        MgcpConnection[] unregistered = unregisterConnections(observer, globalContext);

        if (unregistered.length > 0) {
            // Log deleted calls
            if (log.isDebugEnabled()) {
                String endpointId = globalContext.getEndpointId().toString();
                String hexIdentifiers = Arrays.toString(getConnectionHexId(unregistered));
                int connectionCount = globalContext.getConnections().size();
                log.debug("Endpoint " + endpointId.toString() + " unregistered " + unregistered.length + " connections: " + hexIdentifiers + ". Connection count: " + connectionCount);
            }

            // Let the FSM know that connections were removed
            stateMachine.fire(MgcpEndpointEvent.UNREGISTERED_CONNECTION, context);
        }

        // Send result to callback
        callback.onSuccess(unregistered);
    }

    private MgcpConnection[] unregisterConnections(MgcpEventObserver observer, MgcpEndpointContext globalContext) {
        Collection<MgcpConnection> connections = globalContext.getConnections().values();
        Iterator<MgcpConnection> iterator = connections.iterator();
        List<MgcpConnection> removed = new ArrayList<>(connections.size());

        // Find all connections that belong to then endpoint
        while (iterator.hasNext()) {
            MgcpConnection connection = iterator.next();
            // Remove connection from endpoint
            iterator.remove();
            removed.add(connection);

            // Stop observing the connection
            connection.forget(observer);
        }

        // Return list of unregistered connections
        return removed.toArray(new MgcpConnection[removed.size()]);
    }

    private String[] getConnectionHexId(MgcpConnection[] connections) {
        String[] hex = new String[connections.length];
        int index = 0;
        for (MgcpConnection connection : connections) {
            hex[index] = connection.getHexIdentifier();
            index++;
        }
        return hex;
    }

}
