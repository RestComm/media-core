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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RespondSucessActionTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testRespondSuccessWithLocalDescription() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters requestParameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, requestParameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final String localSdp = "local-sdp-mock";
        
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);

        context.setCallback(callback);
        context.setLocalDescription(localSdp);

        // when
        final RespondSuccessAction action = new RespondSuccessAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.FAILED, ModifyConnectionEvent.FAILURE, context, stateMachine);

        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback).onSuccess(captor.capture());
        
        final MgcpCommandResult result = captor.getValue();
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertEquals(localSdp, responseParameters.getString(MgcpParameterType.SDP).get());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRespondSuccessWithoutLocalDescription() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters requestParameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, requestParameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final FutureCallback<MgcpCommandResult> callback = mock(FutureCallback.class);
        
        context.setCallback(callback);
        
        // when
        final RespondSuccessAction action = new RespondSuccessAction();
        action.execute(ModifyConnectionState.EXECUTING, ModifyConnectionState.FAILED, ModifyConnectionEvent.FAILURE, context, stateMachine);
        
        // then
        final ArgumentCaptor<MgcpCommandResult> captor = ArgumentCaptor.forClass(MgcpCommandResult.class);
        verify(callback).onSuccess(captor.capture());
        
        final MgcpCommandResult result = captor.getValue();
        final Parameters<MgcpParameterType> responseParameters = result.getParameters();
        assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        assertNull(responseParameters.getString(MgcpParameterType.SDP).orNull());
    }


}
