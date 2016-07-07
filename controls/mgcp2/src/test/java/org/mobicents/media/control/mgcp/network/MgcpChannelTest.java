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

package org.mobicents.media.control.mgcp.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

import org.junit.Test;
import org.mobicents.media.control.mgcp.message.MgcpMessageMediator;
import org.mobicents.media.server.io.network.UdpManager;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpChannelTest {

    private final SocketAddress bindAddress = new InetSocketAddress("127.0.0.1", 2427);

    @Test
    public void testOpenClose() throws IllegalStateException, IOException {
        try (DatagramChannel datagramChannel = DatagramChannel.open()) {
            // given
            final SelectionKey selectionKey = mock(SelectionKey.class);
            final UdpManager networkManager = mock(UdpManager.class);
            final MgcpMessageMediator messageCenter = mock(MgcpMessageMediator.class);

            MgcpChannel channel = new MgcpChannel(bindAddress, networkManager, messageCenter);

            // when - open channel
            when(networkManager.open(channel)).thenReturn(selectionKey);
            when(selectionKey.channel()).thenReturn(datagramChannel);
            channel.open();

            // then
            assertTrue(channel.isOpen());
            assertTrue(datagramChannel.isOpen());
            verify(networkManager, times(1)).open(channel);

            // when - channel close
            channel.close();

            // then
            assertFalse(channel.isOpen());
            assertFalse(datagramChannel.isOpen());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
