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

package org.restcomm.media.core.control.mgcp.transaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import org.restcomm.media.core.control.mgcp.command.MgcpCommand;
import org.restcomm.media.core.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.core.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.restcomm.media.core.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.restcomm.media.core.control.mgcp.message.MessageDirection;
import org.restcomm.media.core.control.mgcp.message.MgcpMessageObserver;
import org.restcomm.media.core.control.mgcp.message.MgcpRequest;
import org.restcomm.media.core.control.mgcp.message.MgcpRequestType;
import org.restcomm.media.core.control.mgcp.message.MgcpResponse;
import org.restcomm.media.core.control.mgcp.transaction.MgcpTransaction;
import org.restcomm.media.core.control.mgcp.transaction.MgcpTransactionNumberspace;
import org.restcomm.media.core.control.mgcp.transaction.SubMgcpTransactionManager;

import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SubMgcpTransactionManagerTest {

    private static final String REQUEST = "CRCX 147483653 mobicents/bridge/$@127.0.0.1:2427 MGCP 1.0";
    private static final String RESPONSE = "200 147483653 Successful Transaction";

    @Test
    public void testProcessRemoteTransaction() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int transactionId = 147483653;
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager txManager = new SubMgcpTransactionManager(numberspace, executor);

        // when - request
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);
        when(executor.submit(command)).thenReturn(new AbstractFuture<MgcpCommandResult>() {});
        txManager.process(remote, local, request, command, MessageDirection.INCOMING);

        // then
        assertTrue(txManager.contains(transactionId));
        verify(executor, times(1)).submit(command);

        // when - response
        when(response.toString()).thenReturn(RESPONSE);
        when(response.isRequest()).thenReturn(false);
        when(response.getTransactionId()).thenReturn(transactionId);

        txManager.process(local, remote, response, MessageDirection.OUTGOING);

        // then
        assertFalse(txManager.contains(transactionId));
    }

    @Test
    public void testProcessLocalTransaction() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int initialTransactionId = 0;
        final int finalTransactionId = 147483653;
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager txManager = new SubMgcpTransactionManager(numberspace, executor);

        // when - request
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(initialTransactionId, finalTransactionId);
        when(numberspace.generateId()).thenReturn(147483653);

        txManager.process(local, remote, request, null, MessageDirection.OUTGOING);

        // then
        assertTrue(txManager.contains(finalTransactionId));

        // when - response
        when(response.toString()).thenReturn(RESPONSE);
        when(response.isRequest()).thenReturn(false);
        when(response.getTransactionId()).thenReturn(finalTransactionId);

        txManager.process(remote, local, response, MessageDirection.INCOMING);

        // then
        assertFalse(txManager.contains(finalTransactionId));
    }

    @SuppressWarnings("unchecked")
    @Test(expected = DuplicateMgcpTransactionException.class)
    public void testProcessRetransmission() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int transactionId = 147483653;
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager txManager = new SubMgcpTransactionManager(numberspace, executor);

        // when - request
        when(executor.submit(command)).thenReturn(mock(ListenableFuture.class));
        when(request.toString()).thenReturn(REQUEST);
        when(request.isRequest()).thenReturn(true);
        when(request.getRequestType()).thenReturn(MgcpRequestType.CRCX);
        when(request.getTransactionId()).thenReturn(transactionId);

        txManager.process(remote, local, request, command, MessageDirection.INCOMING);
        txManager.process(remote, local, request, command, MessageDirection.INCOMING);
    }

    @SuppressWarnings("unchecked")
    @Test(expected = MgcpTransactionNotFoundException.class)
    public void testProcessIncomingResponseWithUnknownTransactionId() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int transactionId = 147483653;
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager txManager = new SubMgcpTransactionManager(numberspace, executor);
        
        // when - request
        when(executor.submit(command)).thenReturn(mock(ListenableFuture.class));
        when(response.toString()).thenReturn(REQUEST);
        when(response.isRequest()).thenReturn(true);
        when(response.getTransactionId()).thenReturn(transactionId);
        
        txManager.process(remote, local, response, MessageDirection.INCOMING);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObserverRegistry() {
        // given
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionNumberspace numberspace = new MgcpTransactionNumberspace();
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager transactionManager = new SubMgcpTransactionManager(numberspace, executor);
        final Collection<MgcpMessageObserver> observers = (Collection<MgcpMessageObserver>) Whitebox.getInternalState(transactionManager, "observers");

        // when
        transactionManager.observe(observer);

        // then
        assertEquals(1, observers.size());

        // when
        transactionManager.forget(observer);

        // then
        assertEquals(0, observers.size());

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterDuplicateObserver() {
        // given
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionNumberspace numberspace = new MgcpTransactionNumberspace();
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager transactionManager = new SubMgcpTransactionManager(numberspace, executor);
        final Collection<MgcpMessageObserver> observers = (Collection<MgcpMessageObserver>) Whitebox.getInternalState(transactionManager, "observers");

        // when
        transactionManager.observe(observer);
        transactionManager.observe(observer);

        // then
        assertEquals(1, observers.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessIncomingRequest() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int transactionId = 12345;
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager transactionManager = new SubMgcpTransactionManager(numberspace, executor);
        final Map<Integer, MgcpTransaction> transactions = (Map<Integer, MgcpTransaction>) Whitebox.getInternalState(transactionManager, "transactions");

        // when
        when(executor.submit(command)).thenReturn(mock(ListenableFuture.class));
        when(request.getTransactionId()).thenReturn(transactionId);
        when(response.getTransactionId()).thenReturn(transactionId);

        transactionManager.observe(observer);
        transactionManager.process(remote, local, request, command, MessageDirection.INCOMING);

        // then
        verify(numberspace, never()).generateId();
        assertEquals(1, transactions.size());
        assertNotNull(transactions.get(transactionId));

        // when
        transactionManager.notify(this, local, remote, response, MessageDirection.OUTGOING);

        // then
        verify(observer, only()).onMessage(local, remote, response, MessageDirection.OUTGOING);
        assertEquals(1, transactions.size());
        assertNotNull(transactions.get(transactionId));

        // then
        transactionManager.process(local, remote, response, MessageDirection.OUTGOING);
        assertTrue(transactions.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testProcessOutgoingRequest() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final int transactionId = 12345;
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionNumberspace numberspace = mock(MgcpTransactionNumberspace.class);
        final ListeningExecutorService executor = mock(ListeningExecutorService.class);
        final SubMgcpTransactionManager transactionManager = new SubMgcpTransactionManager(numberspace, executor);
        final Map<Integer, MgcpTransaction> transactions = (Map<Integer, MgcpTransaction>) Whitebox.getInternalState(transactionManager, "transactions");

        // when
        when(numberspace.generateId()).thenReturn(12345);
        when(request.getTransactionId()).thenReturn(0, transactionId);
        when(response.getTransactionId()).thenReturn(transactionId);

        transactionManager.observe(observer);
        transactionManager.process(remote, local, request, null, MessageDirection.OUTGOING);

        // then
        verify(numberspace, only()).generateId();
        assertEquals(1, transactions.size());
        assertNotNull(transactions.get(transactionId));

        // then
        transactionManager.process(local, remote, response, MessageDirection.INCOMING);
        assertTrue(transactions.isEmpty());
    }

}
