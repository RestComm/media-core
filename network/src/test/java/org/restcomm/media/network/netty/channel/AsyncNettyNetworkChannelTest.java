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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.restcomm.media.network.netty.NettyNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelProgressivePromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Channel.class, NioDatagramChannel.class, DatagramChannel.class})
public class AsyncNettyNetworkChannelTest {

    private DatagramChannel remotePeer;
    private EventLoopGroup eventGroup;
    private EventExecutor executor;

    @After
    public void after() {
        if (this.remotePeer != null) {
            if (this.remotePeer.isOpen()) {
                try {
                    this.remotePeer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            this.remotePeer = null;
        }

        if (this.executor != null) {
            if (!this.executor.isShutdown()) {
                this.executor.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }
            this.executor = null;
        }

        if (this.eventGroup != null) {
            this.eventGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            this.eventGroup = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLifecycle() {
        // given
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);

        final ChannelHandler channelHandler = mock(ChannelHandler.class);
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> disconnectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when - open
        networkChannel.open(openCallback);

        // then
        verify(openCallback, timeout(100)).onSuccess(null);
        assertTrue(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());

        // when - bind
        networkChannel.bind(localAddress, bindCallback);

        // then
        verify(bindCallback, timeout(100)).onSuccess(null);
        assertTrue(networkChannel.isOpen());
        assertTrue(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());

        // when - connect
        networkChannel.connect(remoteAddress, connectCallback);

        // then
        verify(connectCallback, timeout(100)).onSuccess(null);
        assertTrue(networkChannel.isOpen());
        assertTrue(networkChannel.isBound());
        assertTrue(networkChannel.isConnected());

        // when - disconnect
        networkChannel.disconnect(disconnectCallback);

        // then
        verify(disconnectCallback, timeout(100)).onSuccess(null);
        assertTrue(networkChannel.isOpen());
        assertTrue(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());

        // when - close
        networkChannel.close(closeCallback);

        // then
        verify(closeCallback, timeout(100)).onSuccess(null);
        assertFalse(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendConnected() throws Exception {
        // given
        final String message = "hello";
        final byte[] data = new byte[message.getBytes().length];
        final ByteBuffer dataBuffer = ByteBuffer.allocate(data.length);

        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);

        final ChannelHandler channelHandler = new ObjectChannelHandler();
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> sendCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when
        this.remotePeer = DatagramChannel.open();
        this.remotePeer.configureBlocking(false);
        this.remotePeer.bind(remoteAddress);

        networkChannel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);

        networkChannel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);

        networkChannel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        networkChannel.send(Unpooled.copiedBuffer(message.getBytes()), sendCallback);
        verify(sendCallback, timeout(100)).onSuccess(null);

        Thread.sleep(20);
        final SocketAddress msgSender = remotePeer.receive(dataBuffer);
        dataBuffer.rewind();
        dataBuffer.get(data);

        this.remotePeer.close();
        networkChannel.close(closeCallback);

        // then
        assertEquals(localAddress, msgSender);
        assertEquals(message, new String(data));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendDisconnected() throws Exception {
        // given
        final String message = "hello";
        final byte[] data = new byte[message.getBytes().length];
        final ByteBuffer dataBuffer = ByteBuffer.allocate(data.length);

        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);

        final ChannelHandler channelHandler = new ObjectChannelHandler();
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> sendCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when
        this.remotePeer = DatagramChannel.open();
        this.remotePeer.configureBlocking(false);
        this.remotePeer.bind(remoteAddress);

        networkChannel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);

        networkChannel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);

        networkChannel.send(Unpooled.copiedBuffer(message.getBytes()), remoteAddress, sendCallback);
        verify(sendCallback, timeout(100)).onSuccess(null);

        Thread.sleep(20);
        final SocketAddress msgSender = remotePeer.receive(dataBuffer);
        dataBuffer.rewind();
        dataBuffer.get(data);

        this.remotePeer.close();
        networkChannel.close(closeCallback);

        // then
        assertEquals(localAddress, msgSender);
        assertEquals(message, new String(data));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testReceive() throws Exception {
        // given
        final String message = "hello";
        final byte[] data = message.getBytes();
        final ByteBuffer dataBuffer = ByteBuffer.allocate(data.length);

        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);

        final ObjectChannelHandler channelHandler = new ObjectChannelHandler();
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<String> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when
        this.remotePeer = DatagramChannel.open();
        this.remotePeer.configureBlocking(false);
        this.remotePeer.bind(remoteAddress);

        networkChannel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);

        networkChannel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);

        dataBuffer.put(data);
        dataBuffer.flip();
        int sent = remotePeer.send(dataBuffer, localAddress);

        Thread.sleep(20);

        this.remotePeer.close();
        networkChannel.close(closeCallback);

        // then
        assertEquals(sent, data.length);
        assertEquals(message, channelHandler.getMessage());
    }

    private class ObjectChannelHandler extends MessageToMessageDecoder<DatagramPacket> {

        private String message = "";

        public String getMessage() {
            return message;
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
            ByteBuf in = msg.content();
            byte[] dst = new byte[in.readableBytes()];
            in.readBytes(dst);
            this.message = new String(dst);
        }

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendToRemoteWhenDisconnected() throws Exception {
        // given
        final String message = "hello";
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);

        final ChannelHandler channelHandler = new ObjectChannelHandler();
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> sendCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when
        networkChannel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);

        networkChannel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);

        networkChannel.send(Unpooled.copiedBuffer(message.getBytes()), sendCallback);
        networkChannel.close(closeCallback);

        // then
        verify(sendCallback, times(1)).onFailure(any(IllegalStateException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendToSpecificRemoteWhenConnected() throws Exception {
        // given
        final String message = "hello";
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);

        final ChannelHandler channelHandler = new ObjectChannelHandler();
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final FutureCallback<Void> connectCallback = mock(FutureCallback.class);
        final FutureCallback<Void> sendCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        // when
        networkChannel.open(openCallback);
        verify(openCallback, timeout(100)).onSuccess(null);

        networkChannel.bind(localAddress, bindCallback);
        verify(bindCallback, timeout(100)).onSuccess(null);

        networkChannel.connect(remoteAddress, connectCallback);
        verify(connectCallback, timeout(100)).onSuccess(null);

        networkChannel.send(Unpooled.copiedBuffer(message.getBytes()), remoteAddress, sendCallback);
        networkChannel.close(closeCallback);

        // then
        verify(sendCallback, times(1)).onFailure(any(IllegalStateException.class));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testOpenFailure() {
        // given
        this.eventGroup = new NioEventLoopGroup();
        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);
        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final Exception exception = new RuntimeException("Testing purposes!");

        // when - open
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final FutureCallback callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onFailure(exception);
                return null;
            }

        }).when(networkManager).openChannel(any(FutureCallback.class));
        networkChannel.open(openCallback);

        // then
        verify(openCallback, timeout(100)).onFailure(exception);
        assertFalse(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBindFailure() {
        // given
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);

        final ChannelFuture channelBindFuture = mock(ChannelFuture.class);
        final ChannelFuture channelCloseFuture = mock(ChannelFuture.class);
        final Channel channel = mock(Channel.class);
        final ChannelHandler channelHandler = mock(ChannelHandler.class);

        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final NettyNetworkManager networkManagerSpy = spy(networkManager);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManagerSpy);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
        final Exception exception = new RuntimeException("Testing purposes!");

        when(channel.bind(localAddress)).thenReturn(channelBindFuture);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final FutureCallback<Channel> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(channel);
                return null;
            }

        }).when(networkManagerSpy).openChannel(any(FutureCallback.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                final ChannelFutureListener listener = invocation.getArgumentAt(0, ChannelFutureListener.class);
                final ChannelPromise promise = new DefaultChannelProgressivePromise(channel, mock(EventExecutor.class));
                promise.setFailure(exception);
                listener.operationComplete(promise);
                return null;
            }

        }).when(channelBindFuture).addListener(any(ChannelFutureListener.class));
        when(channel.close()).thenReturn(channelCloseFuture);

        // when - open
        networkChannel.open(openCallback);
        networkChannel.bind(localAddress, bindCallback);

        // then
        verify(bindCallback, timeout(100)).onFailure(exception);
        assertFalse(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

    // @SuppressWarnings("unchecked")
    // @Test
    // public void testConnectAsyncFailure() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // this.executor = new DefaultEventExecutor();
    // final ChannelPromise bindPromise = new DefaultChannelPromise(channel, executor);
    // final ChannelPromise connectPromise = new DefaultChannelPromise(channel, executor);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    // final Exception exception = new RuntimeException("Testing purposes!");
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(null);
    // when(channel.bind(localAddress)).thenReturn(bindPromise);
    // when(channel.connect(remoteAddress)).thenReturn(connectPromise);
    // bindPromise.setSuccess();
    // connectPromise.setFailure(exception);
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    // networkChannel.connect(remoteAddress, callback);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // verify(callback, timeout(50)).onFailure(exception);
    //
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }
}
