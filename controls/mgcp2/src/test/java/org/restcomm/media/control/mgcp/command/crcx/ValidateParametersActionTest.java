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
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
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
    public void testValidateRequestToCreateTwoLocalConnections() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(CreateConnectionEvent.VALIDATED_PARAMETERS, context);
        assertNull(context.getError());
    }

    @Test
    public void testValidateRequestToCreateRemoteConnection() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        final String remoteSdp = "remote-sdp-mock";
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        parameters.put(MgcpParameterType.SDP, remoteSdp);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fire(CreateConnectionEvent.VALIDATED_PARAMETERS, context);
        assertNull(context.getError());
    }

    @Test
    public void testValidateRequestWhenRemoteDescriptionAndSecondaryEndpointExist() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);

        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        final String remoteSdp = "remote-sdp-mock";
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        parameters.put(MgcpParameterType.SDP, remoteSdp);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenCallIdMissing() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INCORRECT_CALL_ID.code(), error.getCode());
    }
    
    @Test
    public void testValidateRequestWhenPrimaryEndpointIdMissing() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), error.getCode());
    }
    
    @Test
    public void testValidateRequestWhenPrimaryEndpointIsNotFound() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(null);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenSecondaryEndpointIsNotFound() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(null);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), error.getCode());
    }
    
    @Test
    public void testValidateRequestWhenPrimaryEndpointIdIsTooComplicated() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/*", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenSecondaryEndpointIdIsTooComplicated() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/*", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = ConnectionMode.SEND_RECV.description();
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), error.getCode());
    }
    
    @Test
    public void testValidateRequestWhenModeIsMissing() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);

        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);

        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), error.getCode());
    }

    @Test
    public void testValidateRequestWhenModeIsUnknown() {
        // given
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final int transactionId = 6;
        final MgcpCommandParameters parameters = new MgcpCommandParameters();
        final CreateConnectionContext context = new CreateConnectionContext(connectionProvider, endpointManager, transactionId, parameters);
        final CreateConnectionFsm stateMachine = mock(CreateConnectionFsm.class);
        
        final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("restcomm/mock/1", "127.0.0.1:2427");
        final MgcpEndpoint primaryEndpoint = mock(MgcpEndpoint.class);
        when(primaryEndpoint.getEndpointId()).thenReturn(primaryEndpointId);
        
        final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("restcomm/mock/2", "127.0.0.1:2427");
        final MgcpEndpoint secondaryEndpoint = mock(MgcpEndpoint.class);
        when(secondaryEndpoint.getEndpointId()).thenReturn(secondaryEndpointId);
        
        final String callIdHex = "A3";
        final String mode = "xywz";
        
        parameters.put(MgcpParameterType.CALL_ID, callIdHex);
        parameters.put(MgcpParameterType.ENDPOINT_ID, primaryEndpointId.toString());
        parameters.put(MgcpParameterType.SECOND_ENDPOINT, secondaryEndpointId.toString());
        parameters.put(MgcpParameterType.MODE, mode);
        
        when(endpointManager.getEndpoint(primaryEndpointId.toString())).thenReturn(primaryEndpoint);
        when(endpointManager.getEndpoint(secondaryEndpointId.toString())).thenReturn(secondaryEndpoint);
        
        // when
        ValidateParametersAction action = new ValidateParametersAction();
        action.execute(null, CreateConnectionState.VALIDATING_PARAMETERS, CreateConnectionEvent.EXECUTE, context, stateMachine);
        
        // then
        verify(stateMachine).fireImmediate(CreateConnectionEvent.FAILURE, context);
        assertNotNull(context.getError());
        assertTrue(context.getError() instanceof MgcpCommandException);
        MgcpCommandException error = (MgcpCommandException) context.getError();
        assertEquals(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(), error.getCode());
    }

}
