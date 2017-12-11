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

package org.restcomm.media.control.mgcp.command.mdcx;

import org.restcomm.media.control.mgcp.connection.MgcpConnection;

/**
 * Action that updates the mode of a connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class NegotiateConnectionAction extends ModifyConnectionAction {

    static final NegotiateConnectionAction INSTANCE = new NegotiateConnectionAction();

    NegotiateConnectionAction() {
        super();
    }

    @Override
    public void execute(ModifyConnectionState from, ModifyConnectionState to, ModifyConnectionEvent event, ModifyConnectionContext context, ModifyConnectionFsm stateMachine) {
        final MgcpConnection connection = context.getConnection();
        final String remoteDescription = context.getRemoteDescription();

        // Modify connection
        NegotiateConnectionCallback callback = new NegotiateConnectionCallback(stateMachine, context);
        connection.negotiate(remoteDescription, callback);

        // callback will handle rest of logic
    }

}
