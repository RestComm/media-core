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

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Action that sends failure notification about MGCP Command execution.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RespondFailureAction extends ModifyConnectionAction {

    private static final Logger log = Logger.getLogger(RespondFailureAction.class);

    static final RespondFailureAction INSTANCE = new RespondFailureAction();

    public RespondFailureAction() {
        super();
    }

    @Override
    public void execute(ModifyConnectionState from, ModifyConnectionState to, ModifyConnectionEvent event, ModifyConnectionContext context, ModifyConnectionFsm stateMachine) {
        final int transactionId = context.getTransactionId();
        final Throwable error = context.getError();
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final FutureCallback<MgcpCommandResult> callback = context.getCallback();

        final MgcpCommandResult result;
        if (error instanceof MgcpCommandException) {
            MgcpCommandException commandError = (MgcpCommandException) error;
            result = new MgcpCommandResult(transactionId, commandError.getCode(), commandError.getMessage(), parameters);
        } else {
            MgcpResponseCode responseCode = MgcpResponseCode.PROTOCOL_ERROR;
            result = new MgcpCommandResult(transactionId, responseCode.code(), responseCode.message(), parameters);
        }

        log.error("An error occurred during tx=" + transactionId + " execution. Reason: " + error.getMessage() + ". Rolled back.");

        callback.onSuccess(result);
    }

}
