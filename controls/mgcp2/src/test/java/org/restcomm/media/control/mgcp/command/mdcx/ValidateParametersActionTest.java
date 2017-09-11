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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.MgcpCommandParameters;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ValidateParametersActionTest {
    
    @Test
    public void testValidateParameters() {
        // given
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final String mode = ConnectionMode.SEND_ONLY.description();
        final String remoteSdp = "remote-sdp-mock";
        
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getIdentifier()).thenReturn(connectionId);

        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        when(endpoint.getConnection(callId, connectionId)).thenReturn(connection);

        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.SDP, remoteSdp);
        parameters.put(MgcpParameterType.MODE, mode);
        
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.VALIDATED_PARAMETERS, context);
        assertEquals(callId, context.getCallId());
        assertEquals(connectionId, context.getConnectionId());
        assertEquals(connection, context.getConnection());
        assertEquals(endpointId.toString(), context.getEndpointId().toString());
        assertEquals(endpoint, context.getEndpoint());
        assertEquals(ConnectionMode.SEND_ONLY, context.getMode());
        assertEquals(remoteSdp, context.getRemoteDescription());
        assertNull(context.getError());
    }
    
    
    @Test
    public void testValidateRequestWhenCallIdMissing() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenEndpointIdMissing() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, null);
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenEndpointHasAnyWildcard() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/$", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenEndpointHasAllWildcard() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/*", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenEndpointNotFound() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), error.getCode());
    }
    
    @Test
    public void testValidateRequestWhenConnectionIdIsMissing() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, null);
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenConnectionNotFound() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        when(endpoint.getConnection(callId, connectionId)).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INCORRECT_CONNECTION_ID.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenModeIsInvalid() {
        // given
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final int callId = 5;
        final int connectionId = 8;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final ModifyConnectionContext context = new ModifyConnectionContext(transactionId, parameters, endpointManager);
        final ModifyConnectionFsm stateMachine = mock(ModifyConnectionFsm.class);
        
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        when(endpoint.getEndpointId()).thenReturn(endpointId);
        
        final String mode = "unknown-mode";
        
        parameters.put(MgcpParameterType.CALL_ID, String.valueOf(callId));
        parameters.put(MgcpParameterType.CONNECTION_ID, String.valueOf(connectionId));
        parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(endpointId.toString())).thenReturn(endpoint);
        when(endpoint.getConnection(callId, connectionId)).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, ModifyConnectionState.VALIDATING_PARAMETERS, ModifyConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(ModifyConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), error.getCode());
    }

}
