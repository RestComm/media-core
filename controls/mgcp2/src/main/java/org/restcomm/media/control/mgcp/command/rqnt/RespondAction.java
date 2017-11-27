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

package org.restcomm.media.control.mgcp.command.rqnt;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 24/11/2017
 */
class RespondAction extends RequestNotificationAction {

    static final RespondAction INSTANCE = new RespondAction();

    private static final Logger log = Logger.getLogger(RespondAction.class);

    @Override
    public void execute(RequestNotificationState from, RequestNotificationState to, RequestNotificationEvent event, RequestNotificationContext context, RequestNotificationFsm stateMachine) {
        final Throwable error = context.getError();
        final int transactionId = context.getTransactionId();
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final FutureCallback<MgcpCommandResult> callback = context.getCallback();

        final MgcpCommandResult result;

        if(error == null) {
            result = new MgcpCommandResult(transactionId, MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message(), parameters);
        } else {
            if(error instanceof MgcpCommandException) {
                MgcpCommandException commandError = (MgcpCommandException) error;
                result = new MgcpCommandResult(transactionId, commandError.getCode(), commandError.getMessage(), parameters);
                log.error("An error occurred during tx=" + transactionId + " execution. Reason: " + error.getMessage() + ". Aborted.");
            } else {
                result = new MgcpCommandResult(transactionId, MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message(), parameters);
                log.error("An unexpected error occurred during tx=" + transactionId + " execution. Reason: " + error.getMessage() + ". Aborted.", error);
            }
        }

        callback.onSuccess(result);
    }

}
