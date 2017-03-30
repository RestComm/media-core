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

package org.restcomm.media.network.netty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelProgressivePromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FailedFuture;
import io.netty.util.concurrent.Future;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bootstrap.class})
public class NettyNetworkManagerTest {

    private EventExecutor eventExecutor;
    private EventLoopGroup eventLoopGroup;

    @After
    public void cleanup() {
        if (this.eventLoopGroup != null) {
            if (!this.eventLoopGroup.isShutdown()) {
                this.eventLoopGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }
            this.eventLoopGroup = null;
        }

        if (this.eventExecutor != null) {
            if (!this.eventExecutor.isShutdown()) {
                this.eventExecutor.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }
            this.eventExecutor = null;
        }
    }

    @Test
    public void testCloseSync() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);

        // when
        networkManager.close();

        // then
        assertTrue(eventLoopGroup.isShutdown());
    }

    @Test(expected = IOException.class)
    public void testCloseSyncFailure() throws Exception {
        // given
        final EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);

        // when
        when(eventLoopGroup.shutdownGracefully(any(Long.class), any(Long.class), any(TimeUnit.class))).thenThrow(new RuntimeException("Testing purposes!"));
        networkManager.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCloseAsync() {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);

        // when
        networkManager.close(callback);

        // then
        verify(callback, timeout((NettyNetworkManager.SHUTDOWN_TIMEOUT + 1) * 1000)).onSuccess(null);
        assertTrue(eventLoopGroup.isShutdown());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCloseAsyncFailure() {
        // given
        final EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final Exception exception = new RuntimeException("Testing purposes!");
        this.eventExecutor = new DefaultEventExecutor();
        final Future shutdownFuture = new FailedFuture<>(this.eventExecutor, exception);

        // when
        when(eventLoopGroup.shutdownGracefully(any(Long.class), any(Long.class), any(TimeUnit.class))).thenReturn(shutdownFuture);
        networkManager.close(callback);

        // then
        verify(callback, timeout(100)).onFailure(exception);
    }

    @Test
    public void testOpenChannelSync() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final ChannelHandler channelHandler = mock(ChannelHandler.class);
        final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class).handler(channelHandler);
        try (final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap)) {
            // when
            final Channel channel = networkManager.openChannel();

            // then
            assertTrue(channel.isOpen());
            assertTrue(channel.isRegistered());
            assertFalse(channel.isActive());
        }
    }

    @Test(expected = IOException.class)
    public void testOpenChannelSyncFailure() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        final Exception exception = new RuntimeException("Testing purposes!");
        try (final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap)) {
            // when
            when(bootstrap.clone()).thenReturn(bootstrap);
            when(bootstrap.register()).thenThrow(exception);

            networkManager.openChannel();
        }
    }

    @SuppressWarnings({ "unchecked" })
    @Test
    public void testOpenChannelAsync() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final ChannelHandler channelHandler = mock(ChannelHandler.class);
        final Bootstrap bootstrap = new Bootstrap().group(eventLoopGroup).channel(NioDatagramChannel.class).handler(channelHandler);
        this.eventExecutor = new DefaultEventExecutor();
        final FutureCallback<Channel> callback = mock(FutureCallback.class);
        final ArgumentCaptor<Channel> captor = ArgumentCaptor.forClass(Channel.class);

        try (final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap)) {
            // when
            networkManager.openChannel(callback);

            // then
            verify(callback, timeout(100)).onSuccess(captor.capture());
            assertTrue(captor.getValue().isOpen());
            assertTrue(captor.getValue().isRegistered());
            assertFalse(captor.getValue().isActive());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOpenChannelAsyncFailure() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = PowerMockito.mock(Bootstrap.class);
        PowerMockito.when(bootstrap.group()).thenReturn(eventLoopGroup);
        this.eventExecutor = new DefaultEventExecutor();
        final FutureCallback<Channel> callback = mock(FutureCallback.class);
        final Channel channel = mock(Channel.class);
        final ChannelPromise promise = new DefaultChannelProgressivePromise(channel, eventExecutor);
        final Exception exception = new RuntimeException("Testing purposes!");

        try (final NettyNetworkManager networkManager = new NettyNetworkManager(bootstrap)) {
            // when
            when(bootstrap.clone()).thenReturn(bootstrap);
            when(bootstrap.register()).thenReturn(promise);
            promise.setFailure(exception);

            networkManager.openChannel(callback);

            // then
            verify(callback, timeout(100)).onFailure(exception);
        }
    }

}
