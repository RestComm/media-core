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

package org.restcomm.media.control.mgcp.command.crcx;

import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * MGCP command used to create a connection between two endpoints.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommand implements MgcpCommand {

    private final CreateConnectionContext context;
    private final CreateConnectionFsm fsm;

    public CreateConnectionCommand(CreateConnectionContext context, CreateConnectionFsm fsm) {
        this.context = context;
        this.fsm = fsm;
    }

    @Override
    public void execute(FutureCallback<MgcpCommandResult> callback) {
        CreateConnectionEvent event = CreateConnectionEvent.EXECUTE;
        if (this.fsm.canAccept(event)) {
            // Start executing command
            this.fsm.fire(event, context);
        } else {
            // Abort command since FSM does not accept it
            int transactionId = context.getTransactionId();
            MgcpResponseCode response = MgcpResponseCode.ABORTED;
            MgcpCommandParameters parameters = new MgcpCommandParameters();
            MgcpCommandResult result = new MgcpCommandResult(transactionId, response.code(), response.message(), parameters);
            callback.onSuccess(result);
        }
    }

}
