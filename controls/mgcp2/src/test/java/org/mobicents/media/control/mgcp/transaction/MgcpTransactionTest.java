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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.control.mgcp.listener.MgcpTransactionListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.message.MgcpResponse;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionTest {

    private static final String REQUEST = "CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0";
    private static final String RESPONSE = "200 147483655 Successful Transaction";

    @Test
    public void testInboundRequest() {
        // given
        MgcpRequest request = mock(MgcpRequest.class);
        MgcpResponse response = mock(MgcpResponse.class);
        MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        MgcpCommand command = mock(MgcpCommand.class);
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionListener txListener = mock(MgcpTransactionListener.class);

        MgcpTransaction transaction = new MgcpTransaction(commands, messageListener, txListener);
        transaction.setId(12345);

        // when - process incoming request
        when(response.toString()).thenReturn(RESPONSE);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(commands.provide(MgcpRequestType.CRCX)).thenReturn(command);
        when(command.execute(request)).thenReturn(response);
        transaction.processRequest(request, MessageDirection.INBOUND);

        // then
        assertEquals(12345, transaction.getId());
        assertEquals(Integer.toHexString(12345), transaction.getHexId());
        verify(command, times(1)).execute(request);
        assertEquals(MgcpTransactionState.EXECUTING_REQUEST, transaction.getState());

        // when - Command finishes executing
        transaction.onCommandExecuted(response);

        // then
        assertEquals(MgcpTransactionState.COMPLETED, transaction.getState());
        verify(txListener, times(1)).onTransactionComplete(transaction);
        verify(messageListener, times(1)).onOutgoingMessage(any(MgcpMessage.class));
    }

    @Test
    public void testOutboundRequest() {
        // given
        MgcpRequest request = mock(MgcpRequest.class);
        MgcpResponse response = mock(MgcpResponse.class);
        MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionListener txListener = mock(MgcpTransactionListener.class);

        MgcpTransaction transaction = new MgcpTransaction(commands, messageListener, txListener);
        transaction.setId(12345);

        // when - process outbound request
        when(request.toString()).thenReturn(REQUEST);
        transaction.processRequest(request, MessageDirection.OUTBOUND);

        // then
        assertEquals(12345, transaction.getId());
        assertEquals(Integer.toHexString(12345), transaction.getHexId());
        assertEquals(MgcpTransactionState.WAITING_RESPONSE, transaction.getState());
        verify(messageListener, times(1)).onOutgoingMessage(any(MgcpMessage.class));

        // when - response arrives
        transaction.processResponse(response);

        // then
        assertEquals(MgcpTransactionState.COMPLETED, transaction.getState());
        verify(txListener, times(1)).onTransactionComplete(transaction);
    }

    @Test(expected = IllegalStateException.class)
    public void testResponseWhileTransactionIsIdle() {
        // given
        MgcpResponse response = mock(MgcpResponse.class);
        MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionListener txListener = mock(MgcpTransactionListener.class);

        MgcpTransaction transaction = new MgcpTransaction(commands, messageListener, txListener);
        transaction.setId(12345);

        // when - process outbound request
        transaction.processResponse(response);
    }

    @Test(expected = IllegalStateException.class)
    public void testRequestWhileTransactionIsExecuting() {
        // given
        MgcpRequest request1 = mock(MgcpRequest.class);
        MgcpRequest request2 = mock(MgcpRequest.class);
        MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        MgcpCommand command = mock(MgcpCommand.class);
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionListener txListener = mock(MgcpTransactionListener.class);

        MgcpTransaction transaction = new MgcpTransaction(commands, messageListener, txListener);
        transaction.setId(12345);

        // when - process incoming request
        when(request1.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(commands.provide(MgcpRequestType.CRCX)).thenReturn(command);
        transaction.processRequest(request1, MessageDirection.INBOUND);
        transaction.processRequest(request2, MessageDirection.INBOUND);
    }

    @Test(expected = IllegalStateException.class)
    public void testResponseWhileTransactionIsExecuting() {
        // given
        MgcpRequest request = mock(MgcpRequest.class);
        MgcpResponse response = mock(MgcpResponse.class);
        MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        MgcpCommand command = mock(MgcpCommand.class);
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionListener txListener = mock(MgcpTransactionListener.class);

        MgcpTransaction transaction = new MgcpTransaction(commands, messageListener, txListener);
        transaction.setId(12345);

        // when - process incoming request
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(commands.provide(MgcpRequestType.CRCX)).thenReturn(command);
        transaction.processRequest(request, MessageDirection.INBOUND);
        transaction.processResponse(response);
    }

}
