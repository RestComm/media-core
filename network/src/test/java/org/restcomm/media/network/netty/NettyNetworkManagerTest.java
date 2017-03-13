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

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;

import com.google.common.util.concurrent.FutureCallback;

import static org.junit.Assert.*;

import io.netty.bootstrap.Bootstrap;
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
        
        if(this.eventExecutor != null) {
            if(!this.eventExecutor.isShutdown()) {
                this.eventExecutor.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
            }
            this.eventExecutor = null;
        }
    }

    @Test
    public void testCloseSync() throws Exception {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = mock(Bootstrap.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(eventLoopGroup, bootstrap);

        // when
        final long shutdownStart = System.currentTimeMillis();
        networkManager.close();
        final long shutdownStop = System.currentTimeMillis();

        // then
        assertTrue(eventLoopGroup.isShutdown());
        assertEquals(NettyNetworkManager.SHUTDOWN_TIMEOUT, shutdownStop - shutdownStart, 1.0);
    }

    @Test(expected = IOException.class)
    public void testCloseSyncFailure() throws Exception {
        // given
        final EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        final Bootstrap bootstrap = mock(Bootstrap.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(eventLoopGroup, bootstrap);

        // when
        when(eventLoopGroup.shutdownGracefully(any(Long.class), any(Long.class), any(TimeUnit.class))).thenThrow(new RuntimeException("Testing purposes!"));
        networkManager.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCloseAsync() {
        // given
        this.eventLoopGroup = new NioEventLoopGroup(1);
        final Bootstrap bootstrap = mock(Bootstrap.class);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(eventLoopGroup, bootstrap);

        // when
        networkManager.close(callback);

        // then
        verify(callback, after((int) NettyNetworkManager.SHUTDOWN_TIMEOUT)).onSuccess(null);
        assertTrue(eventLoopGroup.isShutdown());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCloseAsyncFailure() {
        // given
        final EventLoopGroup eventLoopGroup = mock(EventLoopGroup.class);
        final Bootstrap bootstrap = mock(Bootstrap.class);
        final NettyNetworkManager networkManager = new NettyNetworkManager(eventLoopGroup, bootstrap);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final Exception exception = new RuntimeException("Testing purposes!");
        this.eventExecutor = new DefaultEventExecutor();
        final Future shutdownFuture = new FailedFuture<>(this.eventExecutor, exception);
        
        // when
        when(eventLoopGroup.shutdownGracefully(any(Long.class), any(Long.class), any(TimeUnit.class))).thenReturn(shutdownFuture);
        networkManager.close(callback);
        
        // then
        verify(callback, times(1)).onFailure(exception);
    }

}
