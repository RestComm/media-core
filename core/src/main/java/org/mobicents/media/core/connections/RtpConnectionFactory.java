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

package org.mobicents.media.core.connections;

import java.util.concurrent.atomic.AtomicInteger;

import org.mobicents.media.core.pooling.PooledObjectFactory;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.spi.dsp.DspFactory;

/**
 * Factory that produces RTP connections.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionFactory implements PooledObjectFactory<RtpConnectionImpl> {

    /**
     * Global ID generator for RTP connections
     */
    private static final AtomicInteger ID = new AtomicInteger(1);

    private final ChannelsManager connectionFactory;
    private final DspFactory dspFactory;

    public RtpConnectionFactory(ChannelsManager connectionFactory, DspFactory dspFactory) {
        this.connectionFactory = connectionFactory;
        this.dspFactory = dspFactory;
    }

    @Override
    public RtpConnectionImpl produce() {
        return new RtpConnectionImpl(ID.getAndIncrement(), connectionFactory, dspFactory);
    }

}
