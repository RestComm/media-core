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

package org.restcomm.media.rtp.jitter;

import org.restcomm.media.rtp.JitterBuffer;
import org.restcomm.media.rtp.JitterBufferFactory;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.scheduler.WallClock;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FixedJitterBufferFactory implements JitterBufferFactory {

    private final WallClock wallClock;
    private final int bufferSize;

    public FixedJitterBufferFactory(WallClock wallClock, int bufferSize) {
        super();
        this.wallClock = wallClock;
        this.bufferSize = bufferSize;
    }

    @Override
    public JitterBuffer build() {
        final RtpClock rtpClock = new RtpClock(this.wallClock);
        final FixedJitterBuffer jitterBuffer = new FixedJitterBuffer(rtpClock, this.bufferSize);
        return jitterBuffer;
    }

}
