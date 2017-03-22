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
        
package org.restcomm.media.network.deprecated.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Test;
import org.restcomm.media.network.deprecated.netty.NetworkManager;
import org.restcomm.media.network.deprecated.netty.UdpNetworkManager;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.DatagramChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UdpNetworkManagerTest {
    
    private NetworkManager manager;
    
    @After
    public void after() {
        if(this.manager != null && this.manager.isActive()) {
            this.manager.deactivate();
        }
        this.manager = null;
    }
    
    @Test
    public void testBindChannel() throws InterruptedException {
        // given
        final String address = "127.0.0.1";
        final int port = 60000;
        final ChannelHandler handler = mock(ChannelHandler.class);
        this.manager = new UdpNetworkManager();

        // when - activate manager and bind channel
        manager.activate();

        final ChannelFuture future = manager.bindDatagramChannel(address, port, handler);
        final DatagramChannel channel = (DatagramChannel) future.sync().channel();
        
        // then
        assertTrue(manager.isActive());
        assertTrue(future.isSuccess());
        assertNotNull(channel);
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());
        assertFalse(channel.isConnected());
        assertEquals(new InetSocketAddress(address, port), channel.localAddress());
        
        // when - deactivate manager
        manager.deactivate();
        Thread.sleep(UdpNetworkManager.SHUTDOWN_TIME * 1000);
        
        // then
        assertFalse(manager.isActive());
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testBindWhileInactive() {
        // given
        final String address = "127.0.0.1";
        final int port = 60000;
        final ChannelHandler handler = mock(ChannelHandler.class);
        final NetworkManager manager = new UdpNetworkManager();
        
        // when
        manager.bindDatagramChannel(address, port, handler);
    }

}
