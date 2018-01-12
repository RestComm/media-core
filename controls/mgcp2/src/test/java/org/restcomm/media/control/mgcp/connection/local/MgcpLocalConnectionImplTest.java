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

package org.restcomm.media.control.mgcp.connection.local;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.control.mgcp.connection.MgcpLocalConnection;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.pkg.MgcpEventProvider;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpLocalConnectionImplTest {

    private final MgcpLocalConnectionFsmBuilder fsmBuilder = new MgcpLocalConnectionFsmBuilder();
    private ListeningScheduledExecutorService executor;
    private MgcpLocalConnection connection;

    @Before
    public void before() {
        this.executor = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
    }

    @After
    @SuppressWarnings("unchecked")
    public void after() {
        if (this.connection != null) {
            this.connection.close(mock(FutureCallback.class));
            this.connection = null;
        }
        this.executor.shutdownNow();
        this.executor = null;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testHalfOpenTimeout() throws InterruptedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final int halfOpenTimeout = 50;
        final int openTimeout = 3600;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, halfOpenTimeout, openTimeout, dataChannel);
        final MgcpEventProvider eventProvider = mock(MgcpEventProvider.class);
        this.connection = new MgcpLocalConnectionImpl(context, eventProvider, this.executor, this.fsmBuilder);

        // when
        FutureCallback<String> halfOpenCallback = mock(FutureCallback.class);
        connection.halfOpen(mock(LocalConnectionOptions.class), halfOpenCallback);

        // then
        verify(halfOpenCallback, timeout(100)).onSuccess(null);
        ListenableScheduledFuture<?> timerFuture = context.getTimerFuture();
        assertNotNull(timerFuture);

        // when
        Thread.sleep(halfOpenTimeout + 5);

        // then
        assertTrue(timerFuture.isDone());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOpenTimeout() throws InterruptedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final int openTimeout = 50;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, openTimeout, dataChannel);
        final MgcpEventProvider eventProvider = mock(MgcpEventProvider.class);
        this.connection = new MgcpLocalConnectionImpl(context, eventProvider, this.executor, this.fsmBuilder);

        // when
        FutureCallback<String> openCallback = mock(FutureCallback.class);
        connection.open("", openCallback);

        // then
        verify(openCallback, timeout(100)).onSuccess(null);
        ListenableScheduledFuture<?> timerFuture = context.getTimerFuture();
        assertNotNull(timerFuture);

        // when
        Thread.sleep(openTimeout + 5);

        // then
        assertTrue(timerFuture.isDone());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoOpenTimeout() throws InterruptedException {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final int openTimeout = 0;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, openTimeout, dataChannel);
        final MgcpEventProvider eventProvider = mock(MgcpEventProvider.class);
        this.connection = new MgcpLocalConnectionImpl(context, eventProvider, this.executor, this.fsmBuilder);
        
        // when
        FutureCallback<String> openCallback = mock(FutureCallback.class);
        connection.open("", openCallback);
        
        // then
        verify(openCallback, timeout(100)).onSuccess(null);
        ListenableScheduledFuture<?> timerFuture = context.getTimerFuture();
        assertNull(timerFuture);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJoinAndUpdateMode() throws Exception {
        // given
        final int identifier = 1;
        final int callIdentifier = 1;
        final LocalDataChannel dataChannel = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context = new MgcpLocalConnectionContext(identifier, callIdentifier, dataChannel);
        final MgcpEventProvider eventProvider = mock(MgcpEventProvider.class);
        this.connection = spy(new MgcpLocalConnectionImpl(context, eventProvider, this.executor, this.fsmBuilder));

        final int identifier2 = 1;
        final int callIdentifier2 = 1;
        final LocalDataChannel dataChannel2 = mock(LocalDataChannel.class);
        final MgcpLocalConnectionContext context2 = new MgcpLocalConnectionContext(identifier2, callIdentifier2, dataChannel2);
        final MgcpLocalConnectionImpl joinee = mock(MgcpLocalConnectionImpl.class);

        when(joinee.getContext()).thenReturn(context2);

        // when
        FutureCallback<String> openCallback = mock(FutureCallback.class);
        connection.open("", openCallback);

        // then
        verify(openCallback, timeout(100)).onSuccess(null);

        // when
        FutureCallback<Void> joinCallback = mock(FutureCallback.class);
        connection.join(joinee, joinCallback);

        // then
        verify(joinCallback, timeout(100)).onSuccess(null);
        verify(connection).join(joinee, joinCallback);
        
        // when
        FutureCallback<String> updateModeCallback = mock(FutureCallback.class);
        ConnectionMode mode = ConnectionMode.SEND_RECV;
        connection.updateMode(mode, updateModeCallback);
        
        // then
        verify(updateModeCallback, timeout(100)).onSuccess(null);
        verify(dataChannel).updateMode(mode);
        assertEquals(mode, context.getMode());
        
        // when
        FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        connection.close(closeCallback);
        
        // then
        verify(closeCallback, timeout(100)).onSuccess(null);
        verify(dataChannel).unjoin();
    }

}
