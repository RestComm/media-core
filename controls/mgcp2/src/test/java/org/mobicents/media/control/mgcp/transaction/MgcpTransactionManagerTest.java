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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.command.MgcpCommandProvider;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpRequestType;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManagerTest {

    private static final String REQUEST = "CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0";
    private static final String RESPONSE = "200 147483653 Successful Transaction";

    /**
     * The mediator creates a new transaction and submits an MGCP command for execution, upon receiving an incoming MGCP
     * request.
     * <p>
     * The mediator shall close the existing transaction upon receiving the outgoing response. The outgoing response must be
     * broadcast to all observers.
     * </p>
     */
    @Test
    public void testIncomingRequest() {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpTransactionProvider txProvider = mock(MgcpTransactionProvider.class);
        final MgcpTransaction transaction = new MgcpTransaction(transactionId);
        final MgcpMessageObserver channel = mock(MgcpMessageObserver.class);
        final MgcpTransactionManager mediator = new MgcpTransactionManager(txProvider);

        // when...then
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(response.toString()).thenReturn(RESPONSE);
        when(response.isRequest()).thenReturn(false);
        when(response.getTransactionId()).thenReturn(transactionId);
        when(txProvider.provideRemote(transactionId)).thenReturn(transaction);
        when(commands.provide(MgcpRequestType.CRCX)).thenReturn(command);

        // execute
        mediator.observe(channel);
        mediator.notify(channel, request, MessageDirection.INCOMING);

        // assert
        assertTrue(mediator.contains(transactionId));
        verify(txProvider, times(1)).provideRemote(transactionId);
        verify(command, times(1)).execute(request, mediator);

        // execute
        mediator.notify(command, response, MessageDirection.OUTGOING);

        // assert
        assertFalse(mediator.contains(transactionId));
        verify(channel, times(1)).onMessage(response, MessageDirection.OUTGOING);
    }

    /**
     * The mediator creates a new transaction upon receiving an outgoing MGCP request from an endpoint (NTFY for example). The
     * message is broadcast and intercepted by the observers.
     * <p>
     * The mediator shall close the existing transaction upon receiving the incoming response.
     * </p>
     */
    @Test
    public void testOutgoingRequest() {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpTransactionProvider txProvider = mock(MgcpTransactionProvider.class);
        final MgcpTransaction transaction = new MgcpTransaction(transactionId);
        final MgcpMessageObserver channel = mock(MgcpMessageObserver.class);
        final MgcpMessageObserver endpoint = mock(MgcpMessageObserver.class);
        final MgcpTransactionManager mediator = new MgcpTransactionManager(txProvider);

        // when...then
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(0, transactionId);
        when(response.toString()).thenReturn(RESPONSE);
        when(response.isRequest()).thenReturn(false);
        when(response.getTransactionId()).thenReturn(transactionId);
        when(txProvider.provideLocal()).thenReturn(transaction);

        // execute
        mediator.observe(channel);
        mediator.notify(endpoint, request, MessageDirection.OUTGOING);

        // assert
        assertTrue(mediator.contains(transactionId));
        verify(txProvider, times(1)).provideLocal();
        verify(channel, times(1)).onMessage(request, MessageDirection.OUTGOING);

        // execute
        mediator.notify(channel, response, MessageDirection.INCOMING);

        // assert
        assertFalse(mediator.contains(transactionId));
    }

    @Test
    public void testRetransmission() {
        // given
        final int transactionId = 147483653;
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommandProvider commands = mock(MgcpCommandProvider.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpTransactionProvider txProvider = mock(MgcpTransactionProvider.class);
        final MgcpTransaction transaction = new MgcpTransaction(transactionId);
        final MgcpMessageObserver channel = mock(MgcpMessageObserver.class);
        final MgcpTransactionManager mediator = new MgcpTransactionManager(txProvider);

        // when...then
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(response.toString()).thenReturn(RESPONSE);
        when(response.isRequest()).thenReturn(false);
        when(response.getTransactionId()).thenReturn(transactionId);
        when(txProvider.provideRemote(transactionId)).thenReturn(transaction);
        when(commands.provide(MgcpRequestType.CRCX)).thenReturn(command);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                // assert
                MgcpMessage message = invocation.getArgumentAt(0, MgcpMessage.class);
                assertTrue(message instanceof MgcpResponse);
                assertEquals(MgcpResponseCode.TRANSACTION_BEING_EXECUTED.code(), ((MgcpResponse) message).getCode());
                return null;
            }

        }).when(channel).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));

        // execute
        mediator.observe(channel);
        mediator.notify(channel, request, MessageDirection.INCOMING);
        mediator.notify(channel, request, MessageDirection.INCOMING);

        // assert
        assertTrue(mediator.contains(transactionId));
        verify(channel, times(1)).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
    }

}
