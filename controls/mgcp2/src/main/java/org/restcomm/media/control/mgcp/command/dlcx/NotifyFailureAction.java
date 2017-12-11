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

import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class NotifyFailureAction extends AnonymousAction<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> {

    private static Logger log = Logger.getLogger(NotifyFailureAction.class);
    
    static final NotifyFailureAction INSTANCE = new NotifyFailureAction();
    
    NotifyFailureAction() {
        super();
    }
    
    @Override
    public void execute(DeleteConnectionState from, DeleteConnectionState to, DeleteConnectionEvent event, DeleteConnectionContext context, DeleteConnectionFsm stateMachine) {
        final int transactionId = context.getTransactionId();
        final FutureCallback<MgcpCommandResult> callback = context.getCallback();
        final Parameters<MgcpParameterType> parameters = new Parameters<>();

        final Throwable error = context.getError();
        final MgcpResponseCode response;
        
        if(error instanceof MgcpCallNotFoundException) {
            log.error("Protocol error occurred during tx=" + transactionId + " execution.", error);
            response = MgcpResponseCode.INCORRECT_CALL_ID;
        } else if(error instanceof MgcpConnectionNotFoundException) {
            log.error("Protocol error occurred during tx=" + transactionId + " execution.", error);
            response = MgcpResponseCode.INCORRECT_CONNECTION_ID;
        } else if (error instanceof MgcpCommandException) {
            log.error("Protocol error occurred during tx=" + transactionId + " execution.", error);
            int code = ((MgcpCommandException) error).getCode();
            response = MgcpResponseCode.fromCode(code);
        } else {
            log.error("Unexpected error occurred during tx=" + transactionId + " execution. Aborted operation.", error);
            response = MgcpResponseCode.PROTOCOL_ERROR;
        }

        final MgcpCommandResult result = new MgcpCommandResult(transactionId, response.code(), response.message(), parameters);
        callback.onSuccess(result);
    }

}
