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
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.network.netty.NettyNetworkChannel.NettyNetworkChannelCallbackListener;

import com.google.common.util.concurrent.FutureCallback;

import io.netty.channel.Channel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class NettyNetworkChannelTest {
    
    @Test
    public void testOpenSync() throws Exception {
        // given
        final Channel channel = mock(Channel.class);
        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
        final NettyNetworkChannel<Object> networkChannel = new NettyNetworkChannel<>(networkManager);
        
        // when
        when(channel.isOpen()).thenReturn(true);
        when(channel.localAddress()).thenReturn(null);
        when(channel.remoteAddress()).thenReturn(null);
        when(networkManager.openChannel()).thenReturn(channel);
        
        networkChannel.open();
        
        // then
        verify(networkManager, times(1)).openChannel();
        assertTrue(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

    @Test(expected=IOException.class)
    public void testOpenSyncFailure() throws Exception {
        // given
        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
        final NettyNetworkChannel<Object> networkChannel = new NettyNetworkChannel<>(networkManager);
        
        // when
        when(networkManager.openChannel()).thenThrow(new IOException("Testing purposes!"));
        networkChannel.open();
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testOpenAsync() throws Exception {
        // given
        final Channel channel = mock(Channel.class);
        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
        final NettyNetworkChannel<Object> networkChannel = new NettyNetworkChannel<>(networkManager);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor.forClass(NettyNetworkChannelCallbackListener.class);
        
        // when
        when(channel.isOpen()).thenReturn(true);
        when(channel.localAddress()).thenReturn(null);
        when(channel.remoteAddress()).thenReturn(null);
        doNothing().when(networkManager).openChannel(managerCaptor.capture());
        
        networkChannel.open(callback);
        managerCaptor.getValue().onSuccess(channel);
        
        // then
        verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
        verify(callback, only()).onSuccess(null);
        
        assertTrue(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testOpenAsyncFailure() throws Exception {
        // given
        final Channel channel = mock(Channel.class);
        final NettyNetworkManager networkManager = mock(NettyNetworkManager.class);
        final NettyNetworkChannel<Object> networkChannel = new NettyNetworkChannel<>(networkManager);
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        final ArgumentCaptor<NettyNetworkChannelCallbackListener> managerCaptor = ArgumentCaptor.forClass(NettyNetworkChannelCallbackListener.class);
        final Exception exception = new RuntimeException("Testing purposes!");
        
        // when
        when(channel.isOpen()).thenReturn(true);
        when(channel.localAddress()).thenReturn(null);
        when(channel.remoteAddress()).thenReturn(null);
        doNothing().when(networkManager).openChannel(managerCaptor.capture());
        
        networkChannel.open(callback);
        managerCaptor.getValue().onFailure(exception);
        
        // then
        verify(networkManager, times(1)).openChannel(managerCaptor.getValue());
        verify(callback, only()).onFailure(exception);
        
        assertFalse(networkChannel.isOpen());
        assertFalse(networkChannel.isBound());
        assertFalse(networkChannel.isConnected());
    }

}
