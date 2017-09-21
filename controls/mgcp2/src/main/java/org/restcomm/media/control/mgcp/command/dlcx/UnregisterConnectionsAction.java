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

package org.restcomm.media.control.mgcp.command.dlcx;

import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class UnregisterConnectionsAction extends AnonymousAction<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> {

    static final UnregisterConnectionsAction INSTANCE = new UnregisterConnectionsAction();
    
    UnregisterConnectionsAction() {
        super();
    }
    
    @Override
    public void execute(DeleteConnectionState from, DeleteConnectionState to, DeleteConnectionEvent event, DeleteConnectionContext context, DeleteConnectionFsm stateMachine) {
        final int callId = context.getCallId();
        final int connectionId = context.getConnectionId();
        final MgcpEndpoint endpoint = context.getEndpoint();

        if (callId == DeleteConnectionContext.NO_CALL_ID) {
            // Unregister all connections from the endpoint
            FutureCallback<MgcpConnection[]> callback = new UnregisterConnectionsCallback(context, stateMachine);
            endpoint.unregisterConnections(callback);
        } else if (connectionId == DeleteConnectionContext.NO_CONNECTION_ID) {
            // Unregister connections from then endpoint bound to a certain call
            FutureCallback<MgcpConnection[]> callback = new UnregisterConnectionsCallback(context, stateMachine);
            endpoint.unregisterConnections(callId, callback);
        } else {
            // Unregister single connection
            FutureCallback<MgcpConnection> callback = new UnregisterConnectionCallback(context, stateMachine);
            endpoint.unregisterConnection(callId, connectionId, callback);
        }
    }

}
