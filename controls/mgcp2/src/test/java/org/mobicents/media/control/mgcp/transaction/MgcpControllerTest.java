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

package org.mobicents.media.control.mgcp.transaction;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.controller.MgcpController;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.network.MgcpChannel;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpControllerTest {

    @Test
    public void testIncomingRequest() throws DuplicateMgcpTransactionException {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(transactionId, request.getRequestType(), request.getParameters())).thenReturn(command);

        controller.onMessage(request, MessageDirection.INCOMING);

        // then
        verify(transactions, times(1)).process(request, command);
    }

    @Test
    public void testIncomingDuplicateRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(transactionId, request.getRequestType(), request.getParameters())).thenReturn(command);
        doThrow(new DuplicateMgcpTransactionException("")).when(transactions).process(request, command);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // then
                MgcpResponse obj = invocation.getArgumentAt(0, MgcpResponse.class);
                Assert.assertEquals(MgcpResponseCode.TRANSACTION_BEING_EXECUTED.code(), obj.getCode());
                return null;
            }
        }).when(channel).send(any(MgcpResponse.class));

        controller.onMessage(request, MessageDirection.INCOMING);

        // then
        verify(transactions, times(1)).process(request, command);
        verify(channel, times(1)).send(any(MgcpResponse.class));
    }

    @Test
    public void testIncomingResponse() throws MgcpTransactionNotFoundException {
        // given
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        controller.onMessage(response, MessageDirection.INCOMING);

        // then
        verify(transactions, times(1)).process(response);
    }

    @Test
    public void testOutgoingRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(transactionId, request.getRequestType(), request.getParameters())).thenReturn(command);

        controller.onMessage(request, MessageDirection.OUTGOING);

        // then
        verify(transactions, times(1)).process(request, null);
        verify(channel, times(1)).send(request);
    }

    @Test
    public void testOutgoingDuplicateRequest() throws DuplicateMgcpTransactionException, IOException {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(commands.provide(transactionId, request.getRequestType(), request.getParameters())).thenReturn(command);
        doThrow(new DuplicateMgcpTransactionException("")).when(transactions).process(request, null);

        controller.onMessage(request, MessageDirection.OUTGOING);

        // then
        verify(transactions, times(1)).process(request, null);
        verify(channel, never()).send(request);
    }

    @Test
    public void testOutgoingResponse() throws MgcpTransactionNotFoundException, IOException {
        // given
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        controller.onMessage(response, MessageDirection.OUTGOING);

        // then
        verify(transactions, times(1)).process(response);
        verify(channel, times(1)).send(response);
    }

    @Test
    public void testOutgoingResponseWithUnknownTransaction() throws MgcpTransactionNotFoundException, IOException {
        // given
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpChannel channel = mock(MgcpChannel.class);
        final TransactionManager transactions = mock(TransactionManager.class);
        final MgcpEndpointManager endpoints = mock(MgcpEndpointManager.class);
        final MgcpController controller = new MgcpController(channel, transactions, endpoints, commands);

        // when
        doThrow(new MgcpTransactionNotFoundException("")).when(transactions).process(response);
        controller.onMessage(response, MessageDirection.OUTGOING);

        // then
        verify(transactions, times(1)).process(response);
        verify(channel, never()).send(response);
    }

}
