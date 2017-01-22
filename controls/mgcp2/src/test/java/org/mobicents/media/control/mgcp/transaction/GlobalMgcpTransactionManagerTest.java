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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.junit.Test;
import org.mobicents.media.control.mgcp.command.MgcpCommand;
import org.mobicents.media.control.mgcp.exception.DuplicateMgcpTransactionException;
import org.mobicents.media.control.mgcp.exception.MgcpTransactionNotFoundException;
import org.mobicents.media.control.mgcp.message.MessageDirection;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mockito.internal.util.reflection.Whitebox;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class GlobalMgcpTransactionManagerTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testObserverRegistry() {
        // given
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionManagerProvider provider = mock(MgcpTransactionManagerProvider.class);
        final GlobalMgcpTransactionManager transactionManager = new GlobalMgcpTransactionManager(provider);
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
        final MgcpTransactionManagerProvider provider = mock(MgcpTransactionManagerProvider.class);
        final GlobalMgcpTransactionManager transactionManager = new GlobalMgcpTransactionManager(provider);
        final Collection<MgcpMessageObserver> observers = (Collection<MgcpMessageObserver>) Whitebox.getInternalState(transactionManager, "observers");

        // when
        transactionManager.observe(observer);
        transactionManager.observe(observer);
        
        // then
        assertEquals(1, observers.size());
    }

    @Test
    public void testProcessIncomingRequest() throws DuplicateMgcpTransactionException {
        // given
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionManager subTransactionManager = mock(MgcpTransactionManager.class);
        final MgcpTransactionManagerProvider provider = mock(MgcpTransactionManagerProvider.class);
        final GlobalMgcpTransactionManager transactionManager = new GlobalMgcpTransactionManager(provider);
        
        // when
        when(provider.provide()).thenReturn(subTransactionManager);
        
        transactionManager.observe(observer);
        transactionManager.process(remote, local, request, command, MessageDirection.INCOMING);
        transactionManager.onMessage(local, remote, response, MessageDirection.OUTGOING);
        
        // then
        verify(provider, only()).provide();
        verify(subTransactionManager, times(1)).observe(transactionManager);
        verify(subTransactionManager, times(1)).process(remote, local, request, command, MessageDirection.INCOMING);
        verify(observer, only()).onMessage(local, remote, response, MessageDirection.OUTGOING);
    }

    @Test
    public void testProcessOutgoingRequest() throws DuplicateMgcpTransactionException, MgcpTransactionNotFoundException {
        // given
        final InetSocketAddress remote = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress local = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpRequest request = mock(MgcpRequest.class);
        final MgcpResponse response = mock(MgcpResponse.class);
        final MgcpCommand command = mock(MgcpCommand.class);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpTransactionManager subTransactionManager = mock(MgcpTransactionManager.class);
        final MgcpTransactionManagerProvider provider = mock(MgcpTransactionManagerProvider.class);
        final GlobalMgcpTransactionManager transactionManager = new GlobalMgcpTransactionManager(provider);
        
        // when
        when(provider.provide()).thenReturn(subTransactionManager);
        
        transactionManager.observe(observer);
        transactionManager.process(local, remote, request, command, MessageDirection.OUTGOING);
        transactionManager.process(remote, local, response, MessageDirection.INCOMING);
        
        // then
        verify(provider, only()).provide();
        verify(subTransactionManager, times(1)).observe(transactionManager);
        verify(subTransactionManager, times(1)).process(local, remote, request, command, MessageDirection.OUTGOING);
        verify(subTransactionManager, times(1)).process(remote, local, response, MessageDirection.INCOMING);
    }

}
