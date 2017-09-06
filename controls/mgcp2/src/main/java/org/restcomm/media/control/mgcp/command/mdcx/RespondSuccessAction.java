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

import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Action that sends successful notification about MGCP Command execution.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RespondSuccessAction extends ModifyConnectionAction {

    static final RespondSuccessAction INSTANCE = new RespondSuccessAction();

    RespondSuccessAction() {
        super();
    }

    @Override
    public void execute(ModifyConnectionState from, ModifyConnectionState to, ModifyConnectionEvent event, ModifyConnectionContext context, ModifyConnectionFsm stateMachine) {
        final int transactionId = context.getTransactionId();
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpResponseCode response = MgcpResponseCode.TRANSACTION_WAS_EXECUTED;

        // Build the result
        MgcpCommandResult result = new MgcpCommandResult(transactionId, response.code(), response.message(), parameters);
        translateContext(context, parameters);

        // Send the result to the callback
        FutureCallback<MgcpCommandResult> callback = context.getCallback();
        callback.onSuccess(result);
    }

    private void translateContext(ModifyConnectionContext context, Parameters<MgcpParameterType> parameters) {
        final String localDescription = context.getLocalDescription();
        if (localDescription != null && !localDescription.isEmpty()) {
            parameters.put(MgcpParameterType.SDP, localDescription);
        }
    }

}
