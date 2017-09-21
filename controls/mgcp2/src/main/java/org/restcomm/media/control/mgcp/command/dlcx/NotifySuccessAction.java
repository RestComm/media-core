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

import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.squirrelframework.foundation.fsm.AnonymousAction;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class NotifySuccessAction extends AnonymousAction<DeleteConnectionFsm, DeleteConnectionState, DeleteConnectionEvent, DeleteConnectionContext> {

    static final NotifySuccessAction INSTANCE = new NotifySuccessAction();
    
    NotifySuccessAction() {
        super();
    }
    
    @Override
    public void execute(DeleteConnectionState from, DeleteConnectionState to, DeleteConnectionEvent event, DeleteConnectionContext context, DeleteConnectionFsm stateMachine) {
        final int transactionId = context.getTransactionId();
        final MgcpResponseCode response = MgcpResponseCode.TRANSACTION_WAS_EXECUTED;
        final FutureCallback<MgcpCommandResult> callback = context.getCallback();

        final Parameters<MgcpParameterType> parameters = new Parameters<>();
        parameters.put(MgcpParameterType.CONNECTION_PARAMETERS, context.getConnectionParams());

        final MgcpCommandResult result = new MgcpCommandResult(transactionId, response.code(), response.message(), parameters);
        callback.onSuccess(result);
    }

}
