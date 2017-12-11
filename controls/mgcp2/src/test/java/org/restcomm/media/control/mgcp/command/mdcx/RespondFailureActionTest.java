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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RespondFailureActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRespondExpectedFailure() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final MgcpResponseCode response = MgcpResponseCode.ENDPOINT_UNKNOWN;
        final MgcpCommandException error = new MgcpCommandException(response);
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        context.setCallback(callback);
        context.setError(error);

        // when
        RespondFailureAction action = new RespondFailureAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.FAILED, ModifyConnectionEvent.FAILURE, context, stateMachine);

        // then
        ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback).onSuccess(captor.capture());
        assertEquals(response.code(), captor.getValue().getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRespondUnexpectedFailure() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final MgcpResponseCode response = MgcpResponseCode.PROTOCOL_ERROR;
        final Exception error = new Exception("test purposes");
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        context.setCallback(callback);
        context.setError(error);
        
        // when
        RespondFailureAction action = new RespondFailureAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.FAILED, ModifyConnectionEvent.FAILURE, context, stateMachine);
        
        // then
        ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback).onSuccess(captor.capture());
        assertEquals(response.code(), captor.getValue().getCode());
    }


}
