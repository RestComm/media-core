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
import org.mobicents.media.control.mgcp.listener.MgcpMessageListener;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessage;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpTransactionManagerTest {

    @Test
    public void testTransactionLifecycle() {
        // given
        final int transactionId = 111111111;
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionProvider txProvider = mock(MgcpTransactionProvider.class);
        MgcpTransactionManager txManager = new MgcpTransactionManager(messageListener, txProvider);
        MgcpRequest request = mock(MgcpRequest.class);
        MgcpTransaction transaction = mock(MgcpTransaction.class);

        // when - create transaction and process message
        when(request.isRequest()).thenReturn(true);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(transaction.getId()).thenReturn(transactionId);
        when(txProvider.provideRemote(transactionId)).thenReturn(transaction);
        txManager.process(request, MessageDirection.INCOMING);

        // then
        assertTrue(txManager.contains(transaction.getId()));
        verify(transaction, times(1)).processRequest(request, MessageDirection.INCOMING);
        verify(transaction, times(1)).addMessageListener(messageListener);
        verify(transaction, times(1)).addTransactionListener(txManager);

        // when - transaction finishes
        txManager.onTransactionComplete(transaction);

        // then
        assertFalse(txManager.contains(transaction.getId()));
        verify(transaction, times(1)).removeMessageListener(messageListener);
        verify(transaction, times(1)).removeTransactionListener(txManager);
    }

    @Test
    public void testHandleRetransmission() {
        // given
        final int transactionId = 111111111;
        MgcpMessageListener messageListener = mock(MgcpMessageListener.class);
        MgcpTransactionProvider txProvider = mock(MgcpTransactionProvider.class);
        MgcpTransactionManager txManager = new MgcpTransactionManager(messageListener, txProvider);
        MgcpRequest request = mock(MgcpRequest.class);
        MgcpRequest retransmission = mock(MgcpRequest.class);
        MgcpTransaction transaction = mock(MgcpTransaction.class);

        // when - create transaction and process message
        when(request.isRequest()).thenReturn(true);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(retransmission.isRequest()).thenReturn(true);
        when(retransmission.getTransactionId()).thenReturn(transactionId);
        when(transaction.getId()).thenReturn(transactionId);
        when(txProvider.provideRemote(transactionId)).thenReturn(transaction);
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                MgcpMessage message = invocation.getArgumentAt(0, MgcpMessage.class);
                assertTrue(message instanceof MgcpResponse);
                assertEquals(MgcpResponseCode.TRANSACTION_BEEN_EXECUTED.code(), ((MgcpResponse) message).getCode());
                return null;
            }
        }).when(messageListener).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
        
        
        txManager.process(request, MessageDirection.INCOMING);
        txManager.process(retransmission, MessageDirection.INCOMING);

        // then
        assertTrue(txManager.contains(transaction.getId()));
        verify(transaction, times(1)).processRequest(request, MessageDirection.INCOMING);
        verify(messageListener, times(1)).onMessage(any(MgcpResponse.class), eq(MessageDirection.OUTGOING));
    }

}
