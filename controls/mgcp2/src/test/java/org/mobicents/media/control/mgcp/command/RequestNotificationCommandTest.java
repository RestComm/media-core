/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.media.control.mgcp.command;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.endpoint.MediaGroup;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.message.MgcpMessageParser;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.mobicents.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationCommandTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequest() throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("pa"), eq(16), any(Map.class), eq(endpoint))).thenReturn(mock(MgcpSignal.class));

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                NotificationRequest rqnt = invocation.getArgumentAt(0, NotificationRequest.class);
                int transactionId = rqnt.getTransactionId();
                String requestId = rqnt.getRequestIdentifier();
                NotifiedEntity notifiedEntity = rqnt.getNotifiedEntity();

                Assert.assertEquals(12345, transactionId);
                Assert.assertEquals("10", requestId);
                Assert.assertNotNull(notifiedEntity);
                Assert.assertEquals("restcomm@10.229.72.130:2727", notifiedEntity.toString());
                Assert.assertEquals("10", requestId);
                Assert.assertTrue(rqnt.isListening("AU/oc"));
                Assert.assertTrue(rqnt.isListening("AU/of"));
                Assert.assertEquals(1, rqnt.countSignals());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNull(rqnt.pollSignal());
                return null;
            }

        }).when(endpoint).requestNotification(any(NotificationRequest.class));

        MgcpCommandResult result = rqnt.call();

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        verify(endpoint, times(1)).requestNotification(any(NotificationRequest.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithMultipleSignals()
            throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append(",");
        builder.append("AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACfe453rb3/ea3422f11.wav it=5)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("pa"), eq(16), any(Map.class), eq(endpoint))).thenReturn(mock(MgcpSignal.class));
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // Assert
                NotificationRequest rqnt = invocation.getArgumentAt(0, NotificationRequest.class);
                int transactionId = rqnt.getTransactionId();
                String requestId = rqnt.getRequestIdentifier();
                NotifiedEntity notifiedEntity = rqnt.getNotifiedEntity();

                Assert.assertEquals(12345, transactionId);
                Assert.assertEquals("10", requestId);
                Assert.assertNotNull(notifiedEntity);
                Assert.assertEquals("restcomm@10.229.72.130:2727", notifiedEntity.toString());
                Assert.assertEquals("10", requestId);
                Assert.assertTrue(rqnt.isListening("AU/oc"));
                Assert.assertTrue(rqnt.isListening("AU/of"));
                Assert.assertEquals(2, rqnt.countSignals());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNotNull(rqnt.pollSignal());
                Assert.assertNull(rqnt.pollSignal());
                return null;
            }

        }).when(endpoint).requestNotification(any(NotificationRequest.class));

        MgcpCommandResult result = rqnt.call();

        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code(), result.getCode());
        verify(endpoint, times(1)).requestNotification(any(NotificationRequest.class));
    }

    @Test
    public void testNotificationRequestWithAnyWildcard() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/$@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestMissingRequestIdentifier() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    public void testNotificationWithMissingEndpoint() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(null);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithUnrecognizedSignalPackage() throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AX/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AX/oc(N),AX/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AX"), eq("pa"), eq(16), any(Map.class), eq(endpoint))).thenThrow(new UnrecognizedMgcpPackageException(""));

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.UNKNOWN_PACKAGE.code(), result.getCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNotificationRequestWithUnrecognizedSignalType() throws MgcpParseException, UnrecognizedMgcpPackageException, UnsupportedMgcpSignalException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/xyz(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);
        when(endpoint.getMediaGroup()).thenReturn(mediaGroup);
        when(signalProvider.provide(eq("AU"), eq("xyz"), eq(16), any(Map.class), eq(endpoint))).thenThrow(new UnsupportedMgcpSignalException(""));

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestWithUnrecognizedPackageOnEvent() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:XYZ/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.UNKNOWN_PACKAGE.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestWithUnrecognizedEvent() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/xyz(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestWithUnrecognizedAction() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(XYZ),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestWithMalformedRequestedEvents() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/pa(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N");
        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenReturn(endpoint);

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

    @Test
    public void testNotificationRequestWithUnexpectedError() throws MgcpParseException {
        // given
        final StringBuilder builder = new StringBuilder("RQNT 12345 mobicents/ivr/10@127.0.0.1:2427 MGCP 1.0").append("\n");
        builder.append("N:restcomm@10.229.72.130:2727").append("\n");
        builder.append("X:10").append("\n");
        builder.append("S:AU/xyz(an=http://127.0.0.1:8080/restcomm/cache/ACae6e420f/5a26d1299.wav it=1)").append("\n");
        builder.append("R:AU/oc(N),AU/of(N)");

        final MgcpMessageParser parser = new MgcpMessageParser();
        final MgcpRequest request = parser.parseRequest(builder.toString());
        final MgcpEndpointManager endpointManager = mock(MgcpEndpointManager.class);
        final MgcpSignalProvider signalProvider = mock(MgcpSignalProvider.class);
        final RequestNotificationCommand rqnt = new RequestNotificationCommand(request.getTransactionId(), request.getParameters(), endpointManager, signalProvider);

        // when
        when(endpointManager.getEndpoint("mobicents/ivr/10@127.0.0.1:2427")).thenThrow(new RuntimeException());

        MgcpCommandResult result = rqnt.call();
        
        // then
        Assert.assertNotNull(result);
        Assert.assertEquals(MgcpResponseCode.PROTOCOL_ERROR.code(), result.getCode());
    }

}
