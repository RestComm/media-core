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

package org.mobicents.media.control.mgcp.command;

import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionCommand extends AbstractMgcpCommand {

    public DeleteConnectionCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager, connectionProvider);
    }

    /**
     * Deletes an active connection.
     * 
     * @param callId The ID of the call where the connection is stored.
     * @param connectionId The connection ID
     * @throws MgcpCallNotFoundException When call with such ID cannot be found.
     * @throws MgcpConnectionNotFound When call does not contain connection with such ID.
     */
    private void deleteConnection(int callId, int connectionId) throws MgcpCallNotFoundException, MgcpConnectionNotFound {
        // TODO implement deleteConnection(int callId, int connectionId)
    }

    /**
     * Deletes all currently active connections.
     */
    private void deleteConnections() {
        // TODO implement deleteConnections()
    }

    /**
     * Deletes all currently active connections within a specific call.
     * 
     * @param callId the call identifier
     * @throws MgcpCallNotFoundException When call with such ID cannot be found.
     */
    private void deleteConnections(int callId) throws MgcpCallNotFoundException {
        // TODO implement deleteConnections(int callId)
    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        // TODO Auto-generated method stub
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.ABORTED.code());
        response.setMessage("Not yet implemented");
        response.setTransactionId(request.getTransactionId());
        return response;
    }

    @Override
    protected MgcpResponse rollback(int transactionId, int code, String message) {
        // TODO Auto-generated method stub
        return null;
    }

}
