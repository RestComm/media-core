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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link NetworkGuard} implementation that accepts first packet from any source. From that moment on, the channel connects to
 * remote peer and only traffic coming from that single source is accpeted.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SbcNetworkGuard implements NetworkGuard {

    private static final Logger log = LogManager.getLogger(SbcNetworkGuard.class);

    @Override
    public boolean isSecure(NetworkChannel channel, InetSocketAddress source) {
        if (channel.isConnected()) {
            return channel.getRemoteAddress().equals(source);
        } else {
            try {
                channel.connect(source);
                return true;
            } catch (IOException e) {
                log.warn("Could not connect channel", e);
                return false;
            }
        }
    }

}
