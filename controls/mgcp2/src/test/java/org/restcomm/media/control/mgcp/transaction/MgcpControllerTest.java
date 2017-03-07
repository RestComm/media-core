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

package org.restcomm.media.control.mgcp.transaction;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandProvider;
import org.restcomm.media.control.mgcp.controller.MgcpController;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.restcomm.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpRequest;
import org.restcomm.media.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.control.mgcp.message.MgcpResponse;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.network.MgcpChannel;
import org.restcomm.media.control.mgcp.transaction.MgcpTransactionManager;
import org.restcomm.media.network.UdpManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpControllerTest {

    @Test
    public void testIncomingRequest() throws DuplicateMgcpTransactionException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final int transactionId = 147483653;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MessageDirection direction = MessageDirection.INCOMING;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(request.getRequestType(), transactionId, request.getParameters())).thenReturn(command);

        controller.onMessage(from, to, request, direction);

        // then
        verify(transactions, times(1)).process(from, to, request, command, direction);
    }

    @Test
    public void testIncomingDuplicateRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final int transactionId = 147483653;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MessageDirection direction = MessageDirection.INCOMING;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(request.getRequestType(), transactionId, request.getParameters())).thenReturn(command);
        doThrow(new DuplicateMgcpTransactionException("")).when(transactions).process(from, to, request, command, direction);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse obj = invocation.getArgumentAt(1, MgcpResponse.class);
                Assert.assertEquals(MgcpResponseCode.TRANSACTION_BEING_EXECUTED.code(), obj.getCode());
                return null;
            }
        }).when(channel).send(eq(to), any(MgcpResponse.class));

        controller.onMessage(from, to, request, direction);

        // then
        verify(transactions, times(1)).process(from, to, request, command, direction);
        verify(channel, times(1)).send(eq(to), any(MgcpResponse.class));
    }

    @Test
    public void testIncomingResponse() throws MgcpTransactionNotFoundException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MessageDirection direction = MessageDirection.INCOMING;
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        controller.onMessage(from, to, response, direction);

        // then
        verify(transactions, times(1)).process(from, to, response, direction);
    }

    @Test
    public void testOutgoingRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final int transactionId = 147483653;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2727);
        final MessageDirection direction = MessageDirection.OUTGOING;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(request.getRequestType(), transactionId, request.getParameters())).thenReturn(command);

        controller.onMessage(from, to, request, direction);

        // then
        verify(transactions, times(1)).process(from, to, request, null, direction);
        verify(channel, times(1)).send(to, request);
    }

    @Test
    public void testOutgoingDuplicateRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final int transactionId = 147483653;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2727);
        final MessageDirection direction = MessageDirection.OUTGOING;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(request.getRequestType(), transactionId, request.getParameters())).thenReturn(command);
        doThrow(new DuplicateMgcpTransactionException("")).when(transactions).process(from, to, request, null, direction);

        controller.onMessage(from, to, request, direction);

        // then
        verify(transactions, times(1)).process(from, to, request, null, direction);
        verify(channel, never()).send(to, request);
    }

    @Test
    public void testOutgoingResponse() throws MgcpTransactionNotFoundException, IOException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2727);
        final MessageDirection direction = MessageDirection.OUTGOING;
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        controller.onMessage(from, to, response, direction);

        // then
        verify(transactions, times(1)).process(from, to, response, direction);
        verify(channel, times(1)).send(to, response);
    }

    @Test
    public void testOutgoingResponseWithUnknownTransaction() throws MgcpTransactionNotFoundException, IOException {
        // given
        final String address = "127.0.0.1";
        final int port = 2427;
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2727);
        final MessageDirection direction = MessageDirection.OUTGOING;
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final UdpManager networkManager = mock(UdpManager.class);
        final MgcpTransactionManager transactions = mock(MgcpTransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(address, port, networkManager, channel, transactions, endpoints, commands);

        // when
        doThrow(new MgcpTransactionNotFoundException("")).when(transactions).process(from, to, response, direction);
        controller.onMessage(from, to, response, direction);

        // then
        verify(transactions, times(1)).process(from, to, response, direction);
        verify(channel, never()).send(to, response);
    }

}
