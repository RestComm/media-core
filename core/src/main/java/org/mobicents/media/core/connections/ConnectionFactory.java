/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.core.connections;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Factory that produces {@link Connection} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ConnectionFactory {

    private static final AtomicInteger ID_GENERATOR_RTP = new AtomicInteger(0);
    private static final AtomicInteger ID_GENERATOR_LOCAL = new AtomicInteger(0);

    private final ChannelsManager channelsManager;
    private final DspFactory dspFactory;

    public ConnectionFactory(ChannelsManager channelManager, DspFactory dspFactory) {
        this.channelsManager = channelManager;
        this.dspFactory = dspFactory;
    }

    public Connection createConnection(ConnectionType type, boolean local) {
        switch (type) {
            case LOCAL:
                return createLocalConnection(local);

            case RTP:
                return createRtpConnection(local);

            default:
                throw new IllegalArgumentException("Unknown connection type: " + type);
        }
    }

    private RtpConnectionImpl createRtpConnection(boolean local) {
        RtpConnectionImpl connection = new RtpConnectionImpl(ID_GENERATOR_RTP.incrementAndGet(), channelsManager, dspFactory);
        connection.setIsLocal(local);
        return connection;
    }

    private LocalConnectionImpl createLocalConnection(boolean local) {
        LocalConnectionImpl connection = new LocalConnectionImpl(ID_GENERATOR_LOCAL.incrementAndGet(), channelsManager);
        connection.setIsLocal(local);
        return connection;
    }

}
