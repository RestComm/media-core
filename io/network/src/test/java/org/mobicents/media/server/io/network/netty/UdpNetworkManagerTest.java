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
        
package org.mobicents.media.server.io.network.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Test;
import org.mobicents.media.server.io.network.PortManager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UdpNetworkManagerTest {
    
    private NetworkManager manager;
    
    @After
    public void afterTest() {
        if(this.manager != null && this.manager.isActive()) {
            this.manager.deactivate();
        }
    }
    
    @Test
    public void testBindUdpChannel() {
        // given
        PortManager ports = mock(PortManager.class);
        ChannelHandler handler = mock(ChannelHandler.class);
        NetworkManager manager = new UdpNetworkManager("127.0.0.1", ports);

        // when - activate manager and bind channel
        when(ports.next()).thenReturn(65534);
        manager.activate();
        
        ChannelFuture future = manager.bindChannel(handler);
        Channel channel = future.channel();
        
        try {
            future.sync();
        } catch (InterruptedException e) {
            fail();
        }
        
        // then
        assertTrue(manager.isActive());
        assertTrue(future.isSuccess());
        assertNotNull(channel);
        assertTrue(channel.isOpen());
        assertTrue(channel.isActive());
        assertEquals(new InetSocketAddress("127.0.0.1", 65534), channel.localAddress());
        
        // when - deactivate manager
        manager.deactivate();
        
        // then
        assertFalse(manager.isActive());
        assertFalse(channel.isOpen());
        assertFalse(channel.isActive());
    }
    
    @Test(expected=IllegalStateException.class)
    public void testBindWhileInactive() {
        // given
        PortManager ports = mock(PortManager.class);
        ChannelHandler handler = mock(ChannelHandler.class);
        NetworkManager manager = new UdpNetworkManager("127.0.0.1", ports);
        
        // when
        manager.bindChannel(handler);
    }

}
