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

package org.restcomm.media.network.deprecated.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.network.deprecated.channel.MultiplexedNetworkChannel;
import org.restcomm.media.network.deprecated.channel.NetworkChannel;
import org.restcomm.media.network.deprecated.channel.PacketHandler;

import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestrictedMultiplexedNetworkChannelTest {

    private static final Logger log = LogManager.getLogger(RestrictedMultiplexedNetworkChannelTest.class);

    private static final byte[] ping = "ping".getBytes();
    private static final byte[] pong = "pong".getBytes();

    private final ByteBuffer agentBuffer = ByteBuffer.allocate(200);
    private DatagramChannel callAgent;
    private NetworkChannel channel;

    @Before
    public void before() throws IOException {
        this.callAgent = DatagramChannel.open();
        this.callAgent.configureBlocking(false);
        this.callAgent.bind(new InetSocketAddress("127.0.0.1", 0));
        this.agentBuffer.clear();
    }

    @After
    public void after() {
        if (callAgent != null && callAgent.isOpen()) {
            try {
                callAgent.close();
            } catch (IOException e) {
                log.error("Could not close Call Agent", e);
            }
        }

        if (this.channel != null && this.channel.isOpen()) {
            this.channel.close();
        }
    }

    @Test
    public void testSendReceive() throws Exception {
        // given
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        final NetworkGuard guard = mock(NetworkGuard.class);
        final PacketHandler handler = mock(PacketHandler.class);
        final MultiplexedNetworkChannel channel = new MultiplexedNetworkChannel(guard, handler);

        // when
        channel.open();
        channel.bind(address);

        when(handler.canHandle(ping)).thenReturn(true);
        when(handler.handle(ping, channel.getLocalAddress(), (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(pong);
        when(guard.isSecure(channel, (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(true);

        Thread.sleep(10);
        callAgent.send(ByteBuffer.wrap(ping), channel.getLocalAddress());
        
        Thread.sleep(10);
        channel.receive();
        
        Thread.sleep(10);
        callAgent.receive(agentBuffer);

        final byte[] response = new byte[pong.length];
        agentBuffer.flip();
        agentBuffer.get(response, agentBuffer.position(), agentBuffer.limit());

        // then
        assertEquals("pong", new String(response));
    }

    @Test
    public void testReceiveOnly() throws Exception {
        // given
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        final NetworkGuard guard = mock(NetworkGuard.class);
        final PacketHandler handler = mock(PacketHandler.class);
        final MultiplexedNetworkChannel channel = new MultiplexedNetworkChannel(guard, handler);

        // when
        channel.open();
        channel.bind(address);

        when(handler.canHandle(ping)).thenReturn(true);
        when(handler.handle(ping, channel.getLocalAddress(), (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(null);
        when(guard.isSecure(channel, (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(true);

        callAgent.send(ByteBuffer.wrap(ping), channel.getLocalAddress());
        channel.receive();

        final SocketAddress remotePeer = callAgent.receive(agentBuffer);

        // then
        assertNull(remotePeer);
    }

    @Test
    public void testBlockInsecureSource() throws Exception {
        // given
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        final NetworkGuard guard = mock(NetworkGuard.class);
        final PacketHandler handler = mock(PacketHandler.class);
        final MultiplexedNetworkChannel channel = new MultiplexedNetworkChannel(guard, handler);

        // when
        channel.open();
        channel.bind(address);

        when(handler.canHandle(ping)).thenReturn(true);
        when(handler.handle(ping, channel.getLocalAddress(), (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(pong);
        when(guard.isSecure(channel, (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(false);

        callAgent.send(ByteBuffer.wrap(ping), channel.getLocalAddress());
        channel.receive();

        final SocketAddress remotePeer = callAgent.receive(agentBuffer);

        // then
        assertNull(remotePeer);
    }

    @Test
    public void testBlockUnsupportedPacket() throws Exception {
        // given
        final byte[] ping = "ping".getBytes();
        final byte[] pong = "pong".getBytes();
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 0);
        final NetworkGuard guard = mock(NetworkGuard.class);
        final PacketHandler handler = mock(PacketHandler.class);
        final MultiplexedNetworkChannel channel = new MultiplexedNetworkChannel(guard, handler);

        // when
        channel.open();
        channel.bind(address);

        when(handler.canHandle(ping)).thenReturn(false);
        when(handler.handle(ping, channel.getLocalAddress(), (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(pong);
        when(guard.isSecure(channel, (InetSocketAddress) callAgent.getLocalAddress())).thenReturn(true);

        callAgent.send(ByteBuffer.wrap(ping), channel.getLocalAddress());
        channel.receive();

        final SocketAddress remotePeer = callAgent.receive(agentBuffer);

        // then
        assertNull(remotePeer);
    }

}
