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

import java.net.InetSocketAddress;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.restcomm.media.network.deprecated.channel.DirectNetworkGuard;
import org.restcomm.media.network.deprecated.channel.NetworkChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DirectNetworkGuardTest {

    @Test
    public void testSecureSource() {
        // given
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.22", 2727);
        final NetworkChannel channel = Mockito.mock(NetworkChannel.class);
        final NetworkGuard guard = new DirectNetworkGuard();

        // when
        Mockito.when(channel.isConnected()).thenReturn(true);
        Mockito.when(channel.getRemoteAddress()).thenReturn(remoteAddress);

        final boolean secure = guard.isSecure(channel, remoteAddress);

        // then
        Assert.assertTrue(secure);
    }

    @Test
    public void testInsecureSource() {
        // given
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.22", 2727);
        final InetSocketAddress unknownRemoteAddress = new InetSocketAddress("232.122.55.20", 2727);
        final NetworkChannel channel = Mockito.mock(NetworkChannel.class);
        final NetworkGuard guard = new DirectNetworkGuard();

        // when
        Mockito.when(channel.isConnected()).thenReturn(true);
        Mockito.when(channel.getRemoteAddress()).thenReturn(remoteAddress);

        final boolean secure = guard.isSecure(channel, unknownRemoteAddress);

        // then
        Assert.assertFalse(secure);
    }

    @Test
    public void testInsecureSourceWithDisconnectedChannel() {
        // given
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.22", 2727);
        final NetworkChannel channel = Mockito.mock(NetworkChannel.class);
        final NetworkGuard guard = new DirectNetworkGuard();

        // when
        Mockito.when(channel.isConnected()).thenReturn(false);

        final boolean secure = guard.isSecure(channel, remoteAddress);

        // then
        Assert.assertFalse(secure);
    }

}
