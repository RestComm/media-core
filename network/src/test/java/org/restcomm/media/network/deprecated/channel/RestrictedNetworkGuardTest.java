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
import org.restcomm.media.network.deprecated.channel.NetworkChannel;
import org.restcomm.media.network.deprecated.channel.RestrictedNetworkGuard;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RestrictedNetworkGuardTest {

    @Test
    public void testSecureSource() {
        // given
        final String subnet = "255.255.255.0";
        final String network = "192.168.1.0";
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.1.22", 2727);
        final NetworkChannel channel = Mockito.mock(NetworkChannel.class);
        final NetworkGuard guard = new RestrictedNetworkGuard(network, subnet);

        // when
        final boolean secure = guard.isSecure(channel, remoteAddress);

        // then
        Assert.assertTrue(secure);
    }

    @Test
    public void testInsecureSource() {
        // given
        final String subnet = "255.255.255.0";
        final String network = "192.168.1.0";
        final InetSocketAddress remoteAddress = new InetSocketAddress("192.168.2.4", 2727);
        final NetworkChannel channel = Mockito.mock(NetworkChannel.class);
        final NetworkGuard guard = new RestrictedNetworkGuard(network, subnet);

        // when
        final boolean secure = guard.isSecure(channel, remoteAddress);

        // then
        Assert.assertFalse(secure);
    }

}
