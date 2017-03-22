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

package org.restcomm.media.network.netty.channel;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.network.api.AsynchronousNetworkManager;
import org.squirrelframework.foundation.fsm.StateMachineLogger;
import org.squirrelframework.foundation.fsm.StateMachinePerformanceMonitor;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsyncNettyNetworkChannelFsmTest {

    private NettyNetworkChannelFsm fsm;
    private StateMachineLogger fsmLogger;
    private StateMachinePerformanceMonitor fsmMonitor;

    @After
    public void after() {
        if(fsmLogger != null) {
            this.fsmLogger.stopLogging();
            this.fsmLogger = null;
        }

        if (fsm != null) {
            if (fsm.isStarted()) {
                fsm.terminate();
            }
            
            if(this.fsmMonitor != null) {
                this.fsm.removeDeclarativeListener(fsmMonitor);
                System.out.println(fsmMonitor.getPerfModel());
            }
            
            fsm = null;
        }
        
        this.fsmMonitor = null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testChannelLifecycleStepByStep() {
        // given
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
        final Channel channel = mock(Channel.class);
        final AsynchronousNetworkManager<Channel> networkManager = mock(AsynchronousNetworkManager.class);
        final NettyNetworkChannelGlobalContext globalContext = new NettyNetworkChannelGlobalContext(networkManager);
        this.fsm = NettyNetworkChannelFsmBuilder.INSTANCE.build(globalContext);
        this.fsmLogger = new StateMachineLogger(fsm);
        this.fsmMonitor = new StateMachinePerformanceMonitor("testChannelLifecycleStepByStep");
        
        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> disconnectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        
        final NettyNetworkChannelTransitionContext openContext = new NettyNetworkChannelTransitionContext().setCallback(openCallback);
        final NettyNetworkChannelTransitionContext bindContext = new NettyNetworkChannelTransitionContext().setCallback(bindCallback);
        final NettyNetworkChannelTransitionContext connectContext = new NettyNetworkChannelTransitionContext().setCallback(connectCallback);
        final NettyNetworkChannelTransitionContext disconnectContext = new NettyNetworkChannelTransitionContext().setCallback(disconnectCallback);
        final NettyNetworkChannelTransitionContext closeContext = new NettyNetworkChannelTransitionContext().setCallback(closeCallback);

        final ChannelFuture bindFuture = mock(ChannelFuture.class);
        final ChannelFuture connectFuture = mock(ChannelFuture.class);
        final ChannelFuture disconnectFuture = mock(ChannelFuture.class);
        final ChannelFuture closeFuture = mock(ChannelFuture.class);
        
        doAnswer(new SuccessfulAnswer(openCallback)).when(networkManager).openChannel(any(FutureCallback.class));
        when(channel.bind(eq(localAddress))).thenReturn(bindFuture);
        when(channel.connect(eq(remoteAddress))).thenReturn(connectFuture);
        when(channel.disconnect()).thenReturn(disconnectFuture);
        when(channel.close()).thenReturn(closeFuture);
        
        when(bindFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new SuccessfulAnswer(bindCallback));
        when(connectFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new SuccessfulAnswer(connectCallback));
        when(disconnectFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new SuccessfulAnswer(disconnectCallback));
        when(closeFuture.addListener(any(ChannelFutureListener.class))).thenAnswer(new SuccessfulAnswer(closeCallback));
        
        // when
        fsm.addDeclarativeListener(this.fsmMonitor);
        fsm.start();
        fsmLogger.startLogging();
        fsm.fire(NettyNetworkChannelEvent.OPEN, openContext);

        // then
        assertEquals(NettyNetworkChannelState.OPENING, fsm.getCurrentState());
        verify(networkManager, only()).openChannel(any(FutureCallback.class));
        
        // when
        globalContext.setChannel(channel);
        fsm.fire(NettyNetworkChannelEvent.OPENED);
        
        // then
        assertEquals(NettyNetworkChannelState.OPEN, fsm.getCurrentState());
        
        // when
        globalContext.setLocalAddress(localAddress);
        fsm.fire(NettyNetworkChannelEvent.BIND, bindContext);
        
        // then
        assertEquals(NettyNetworkChannelState.BINDING, fsm.getCurrentState());
        verify(bindCallback, only()).onSuccess(null);
        verify(channel, times(1)).bind(localAddress);
        
        // when
        fsm.fire(NettyNetworkChannelEvent.BOUND);
        
        // then
        assertEquals(NettyNetworkChannelState.BOUND, fsm.getCurrentState());
        
        // when
        globalContext.setRemoteAddress(remoteAddress);
        fsm.fire(NettyNetworkChannelEvent.CONNECT, connectContext);
        
        // then
        assertEquals(NettyNetworkChannelState.CONNECTING, fsm.getCurrentState());
        verify(channel, times(1)).connect(remoteAddress);
        verify(connectCallback, only()).onSuccess(null);
        
        // when
        fsm.fire(NettyNetworkChannelEvent.CONNECTED);
        
        // then
        assertEquals(NettyNetworkChannelState.CONNECTED, fsm.getCurrentState());

        // when
        fsm.fire(NettyNetworkChannelEvent.DISCONNECT, disconnectContext);
        
        // then
        assertEquals(NettyNetworkChannelState.DISCONNECTING, fsm.getCurrentState());
        verify(channel, times(1)).disconnect();
        verify(disconnectCallback, only()).onSuccess(null);
        
        // when
        fsm.fire(NettyNetworkChannelEvent.DISCONNECTED);
        
        // then
        assertEquals(NettyNetworkChannelState.BOUND, fsm.getCurrentState());
        assertNull(globalContext.getRemoteAddress());
        
        // when
        fsm.fire(NettyNetworkChannelEvent.CLOSE, closeContext);
        
        // then
        assertEquals(NettyNetworkChannelState.CLOSING, fsm.getCurrentState());
        verify(channel, times(1)).close();
        verify(closeCallback, only()).onSuccess(null);

        // when
        fsm.fire(NettyNetworkChannelEvent.CLOSED);
        
        // then
        assertEquals(NettyNetworkChannelState.CLOSED, fsm.getCurrentState());
        assertNull(globalContext.getLocalAddress());
        assertTrue(fsm.isTerminated());
    }

    private class SuccessfulAnswer implements Answer<Void> {

        private final FutureCallback<?> callback;

        public SuccessfulAnswer(FutureCallback<?> callback) {
            super();
            this.callback = callback;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            callback.onSuccess(null);
            return null;
        }
    }

}
