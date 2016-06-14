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
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NotifyCommand extends AbstractMgcpCommand {

    public NotifyCommand(MgcpEndpointManager endpointManager, MgcpConnectionProvider connectionProvider) {
        super(endpointManager, connectionProvider);
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

    @Override
    protected void reset() {
        // TODO Auto-generated method stub

    }

}
