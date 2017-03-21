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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.restcomm.media.network.netty.NettyNetworkManager;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.EventExecutor;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
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

    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // @Test
    // public void testOpenAsync() throws Exception {
    // // given
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    // final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor
    // .forClass(NettyNetworkChannelCallbackListener.class);
    //
    // // when
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(null);
    // when(channel.remoteAddress()).thenReturn(null);
    // doNothing().when(networkManager).openChannel(managerCaptor.capture());
    // networkManager.open
    //
    // networkChannel.open(callback);
    // managerCaptor.getValue().onSuccess(channel);
    //
    // // then
    // verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
    // verify(callback, only()).onSuccess(null);
    //
    // assertTrue(networkChannel.isOpen());
    // assertFalse(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }

    @SuppressWarnings("unchecked")
    @Test
    public void testLifecycle() {
        // given
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);

        final ChannelHandler channelHandler = mock(ChannelHandler.class);
        this.eventGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap().group(eventGroup).handler(channelHandler).channel(NioDatagramChannel.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final AsyncNettyNetworkChannel<Object> networkChannel = new AsyncNettyNetworkChannel<>(networkManager);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> bindCallback = mock(FutureCallback.class);
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

    // @SuppressWarnings({ "unchecked", "rawtypes" })
    // @Test
    // public void testOpenAsyncFailure() throws Exception {
    // // given
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    // final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor
    // .forClass(NettyNetworkChannelCallbackListener.class);
    // final Exception exception = new RuntimeException("Testing purposes!");
    //
    // // when
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(null);
    // when(channel.remoteAddress()).thenReturn(null);
    // doNothing().when(networkManager).openChannel(managerCaptor.capture());
    //
    // networkChannel.open(callback);
    // managerCaptor.getValue().onFailure(exception);
    //
    // // then
    // verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
    // verify(callback, only()).onFailure(exception);
    //
    // assertFalse(networkChannel.isOpen());
    // assertFalse(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }
    //
    // @Test
    // public void testBindSync() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    //
    // // when
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(null);
    // when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
    // when(networkManager.openChannel()).thenReturn(channel);
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }
    //
    // @Test(expected = IOException.class)
    // public void testBindSyncFailure() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // final Exception exception = new RuntimeException("Testing purposes!");
    //
    // // when
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(null);
    // when(channel.bind(localAddress)).thenThrow(exception);
    // when(networkManager.openChannel()).thenReturn(channel);
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    // }
    //
    // @SuppressWarnings("unchecked")
    // @Test
    // public void testBindAsync() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // this.executor = new DefaultEventExecutor();
    // final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(null);
    // when(channel.bind(localAddress)).thenReturn(promise);
    // promise.setSuccess();
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress, callback);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // verify(callback, timeout(50)).onSuccess(null);
    //
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }
    //
    // @SuppressWarnings("unchecked")
    // @Test
    // public void testBindAsyncFailure() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // this.executor = new DefaultEventExecutor();
    // final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    // final Exception exception = new RuntimeException("Testing purposes!");
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(null);
    // when(channel.bind(localAddress)).thenReturn(promise);
    // promise.setFailure(exception);
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress, callback);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // verify(callback, only()).onFailure(exception);
    //
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertFalse(networkChannel.isConnected());
    // }
    //
    // @Test
    // public void testConnectSync() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(remoteAddress);
    // when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
    // when(channel.connect(remoteAddress)).thenReturn(mock(ChannelFuture.class));
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    // networkChannel.connect(remoteAddress);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // verify(channel, times(1)).connect(remoteAddress);
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertTrue(networkChannel.isConnected());
    // }
    //
    // @Test(expected = IOException.class)
    // public void testConnectSyncFailure() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // final Exception exception = new RuntimeException("Testing purposes!");
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(remoteAddress);
    // when(channel.bind(localAddress)).thenReturn(mock(ChannelFuture.class));
    // when(channel.connect(remoteAddress)).thenThrow(exception);
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    // networkChannel.connect(remoteAddress);
    // }
    //
    // @SuppressWarnings("unchecked")
    // @Test
    // public void testConnectAsync() throws Exception {
    // // given
    // final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 2427);
    // final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 2727);
    // final Channel channel = mock(Channel.class);
    // final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
    // final AsynchronousNettyNetworkChannel<Object> networkChannel = new AsynchronousNettyNetworkChannel<>(networkManager);
    // this.executor = new DefaultEventExecutor();
    // final ChannelPromise promise = new DefaultChannelPromise(channel, executor);
    // final FutureCallback<Void> callback = mock(FutureCallback.class);
    //
    // // when
    // when(networkManager.openChannel()).thenReturn(channel);
    // when(channel.isOpen()).thenReturn(true);
    // when(channel.localAddress()).thenReturn(localAddress);
    // when(channel.remoteAddress()).thenReturn(remoteAddress);
    // when(channel.bind(localAddress)).thenReturn(promise);
    // when(channel.connect(remoteAddress)).thenReturn(promise);
    // promise.setSuccess();
    //
    // networkChannel.open();
    // networkChannel.bind(localAddress);
    // networkChannel.connect(remoteAddress, callback);
    //
    // // then
    // verify(networkManager, times(1)).openChannel();
    // verify(channel, times(1)).bind(localAddress);
    // verify(callback, timeout(50)).onSuccess(null);
    //
    // assertTrue(networkChannel.isOpen());
    // assertTrue(networkChannel.isBound());
    // assertTrue(networkChannel.isConnected());
    // }
    //
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
