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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.network.netty.NettyNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelPromise;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsyncNettyNetworkChannelTest {

    private EventExecutor executor;
    private NettyNetworkManager networkManager;

    @After
    public void after() {
        if(this.networkManager != null) {
            try {
                this.networkManager.close();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            this.networkManager = null;
        }
        
        if (this.executor != null) {
            if (!this.executor.isShutdown()) {
                this.executor.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }
            this.executor = null;
        }
    }

//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    @Test
//    public void testOpenAsync() throws Exception {
//        // given
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//        final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor
//                .forClass(NettyNetworkChannelCallbackListener.class);
//
//        // when
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(null);
//        when(channel.remoteAddress()).thenReturn(null);
//        doNothing().when(networkManager).openChannel(managerCaptor.capture());
//        networkManager.open
//
//        networkChannel.open(callback);
//        managerCaptor.getValue().onSuccess(channel);
//
//        // then
//        verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
//        verify(callback, only()).onSuccess(null);
//
//        assertTrue(networkChannel.isOpen());
//        assertFalse(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOpenAsync() {
        // given
        FutureCallback<Void> openCallback = mock(FutureCallback.class);
        this.networkManager = new NettyNetworkManager();
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);
        
        // when
        networkChannel.open(openCallback);
        
        // then
        verify(openCallback, timeout(100)).onSuccess(null);
        assertTrue(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    @Test
//    public void testOpenAsyncFailure() throws Exception {
//        // given
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//        final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor
//                .forClass(NettyNetworkChannelCallbackListener.class);
//        final Exception exception = new RuntimeException("Testing purposes!");
//
//        // when
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(null);
//        when(channel.remoteAddress()).thenReturn(null);
//        doNothing().when(networkManager).openChannel(managerCaptor.capture());
//
//        networkChannel.open(callback);
//        managerCaptor.getValue().onFailure(exception);
//
//        // then
//        verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
//        verify(callback, only()).onFailure(exception);
//
//        assertFalse(networkChannel.isOpen());
//        assertFalse(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
//
//    @Test
//    public void testBindSync() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//
//        // when
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(null);
//        when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
//        when(networkManager.openChannel()).thenReturn(channel);
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
//
//    @Test(expected = IOException.class)
//    public void testBindSyncFailure() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        final Exception exception = new RuntimeException("Testing purposes!");
//
//        // when
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(null);
//        when(channel.bind(localAddress)).thenThrow(exception);
//        when(networkManager.openChannel()).thenReturn(channel);
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testBindAsync() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        this.executor = new DefaultEventExecutor();
//        final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(null);
//        when(channel.bind(localAddress)).thenReturn(promise);
//        promise.setSuccess();
//
//        networkChannel.open();
//        networkChannel.bind(localAddress, callback);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        verify(callback, timeout(50)).onSuccess(null);
//
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testBindAsyncFailure() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        this.executor = new DefaultEventExecutor();
//        final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//        final Exception exception = new RuntimeException("Testing purposes!");
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(null);
//        when(channel.bind(localAddress)).thenReturn(promise);
//        promise.setFailure(exception);
//
//        networkChannel.open();
//        networkChannel.bind(localAddress, callback);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        verify(callback, only()).onFailure(exception);
//
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
//
//    @Test
//    public void testConnectSync() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(remoteAddress);
//        when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
//        when(channel.connect(remoteAddress)).thenReturn(mock(ChannelFuture.class));
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//        networkChannel.connect(remoteAddress);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        verify(channel, times(1)).connect(remoteAddress);
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertTrue(networkChannel.isConnected());
//    }
//
//    @Test(expected = IOException.class)
//    public void testConnectSyncFailure() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        final Exception exception = new RuntimeException("Testing purposes!");
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(remoteAddress);
//        when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
//        when(channel.connect(remoteAddress)).thenThrow(exception);
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//        networkChannel.connect(remoteAddress);
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testConnectAsync() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        this.executor = new DefaultEventExecutor();
//        final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(remoteAddress);
//        when(channel.bind(localAddress)).thenReturn(promise);
//        when(channel.connect(remoteAddress)).thenReturn(promise);
//        promise.setSuccess();
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//        networkChannel.connect(remoteAddress, callback);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        verify(callback, timeout(50)).onSuccess(null);
//
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertTrue(networkChannel.isConnected());
//    }
//
//    @SuppressWarnings("unchecked")
//    @Test
//    public void testConnectAsyncFailure() throws Exception {
//        // given
//        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
//        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
//        final Channel channel = mock(Channel.class);
//        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
//        final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
//        this.executor = new DefaultEventExecutor();
//        final ChannelPromise bindPromise = new DefaultChannelPromise(channel, executor);
//        final ChannelPromise connectPromise = new DefaultChannelPromise(channel, executor);
//        final FutureCallback<Void> callback = mock(FutureCallback.class);
//        final Exception exception = new RuntimeException("Testing purposes!");
//
//        // when
//        when(networkManager.openChannel()).thenReturn(channel);
//        when(channel.isOpen()).thenReturn(true);
//        when(channel.localAddress()).thenReturn(localAddress);
//        when(channel.remoteAddress()).thenReturn(null);
//        when(channel.bind(localAddress)).thenReturn(bindPromise);
//        when(channel.connect(remoteAddress)).thenReturn(connectPromise);
//        bindPromise.setSuccess();
//        connectPromise.setFailure(exception);
//
//        networkChannel.open();
//        networkChannel.bind(localAddress);
//        networkChannel.connect(remoteAddress, callback);
//
//        // then
//        verify(networkManager, times(1)).openChannel();
//        verify(channel, times(1)).bind(localAddress);
//        verify(callback, timeout(50)).onFailure(exception);
//
//        assertTrue(networkChannel.isOpen());
//        assertTrue(networkChannel.isBound());
//        assertFalse(networkChannel.isConnected());
//    }
}
