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

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ValidateParametersActionTest {
    
    @Test
    public void testParametersForDeletingSingleConnection() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final int connectionId = 11;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);

        // then
        verify(context).setCallId(callId);
        verify(context).setConnectionId(connectionId);
        verify(context).setEndpointId(endpointId);
        verify(context).setEndpoint(endpoint);
        verify(fsm).fire(DeleteConnectionEvent.VALIDATED_PARAMETERS, context);
    }

    @Test
    public void testParametersForDeletingCallConnections() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);
        
        // then
        verify(context).setCallId(callId);
        verify(context).setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        verify(context).setEndpointId(endpointId);
        verify(context).setEndpoint(endpoint);
        verify(fsm).fire(DeleteConnectionEvent.VALIDATED_PARAMETERS, context);
    }

    @Test
    public void testParametersForDeletingAllConnections() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);
        
        // then
        verify(context).setCallId(DeleteConnectionContext.NO_CALL_ID);
        verify(context).setConnectionId(DeleteConnectionContext.NO_CONNECTION_ID);
        verify(context).setEndpointId(endpointId);
        verify(context).setEndpoint(endpoint);
        verify(fsm).fire(DeleteConnectionEvent.VALIDATED_PARAMETERS, context);
    }
    
    @Test
    public void testParametersWithoutEndpointId() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final int connectionId = 11;
        final String endpointId = "";
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);

        // then
        verify(context).setError(any(MgcpCommandException.class));
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }
    
    @Test
    public void testParametersWithConnectionIdAndWithoutCallId() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int connectionId = 11;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(endpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);

        // then
        verify(context).setError(any(MgcpCommandException.class));
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }
    
    @Test
    public void testParametersWhenEndpointNotFound() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final int connectionId = 11;
        final String endpointId = "restcomm/mock/1@127.0.0.1:2427";
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(null);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);

        // then
        verify(context).setError(any(MgcpCommandException.class));
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

    @Test
    public void testParametersWhenEndpointIdContainsAllWildCard() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final int connectionId = 11;
        final String endpointId = "restcomm/mock/*@127.0.0.1:2427";
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);
        
        // then
        verify(context).setError(any(MgcpCommandException.class));
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

    @Test
    public void testParametersWhenEndpointIdContainsAnyWildCard() {
        // given
        final int transactionId = 12345;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final DeleteConnectionContext context = spy(new DeleteConnectionContext(transactionId, parameters, endpointManager));
        final DeleteConnectionFsm fsm = mock(DeleteConnectionFsm.class);
        
        final int callId = 10;
        final int connectionId = 11;
        final String endpointId = "restcomm/mock/$@127.0.0.1:2427";
        
        parameters.put(MgcpParameterType.CALL_ID, Integer.toHexString(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, Integer.toHexString(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
        
        when(endpointManager.getEndpoint(endpointId)).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, DeleteConnectionState.VALIDATING_PARAMETERS, null, context, fsm);
        
        // then
        verify(context).setError(any(MgcpCommandException.class));
        verify(fsm).fire(DeleteConnectionEvent.FAILURE, context);
    }

}
