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

package org.restcomm.media.rtp;

import static org.junit.Assert.*;

import org.junit.Test;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.WallClock;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpStatisticsTest {

    @Test
    public void testRtpReceived() {
        // given
        final long ssrc = 12345;
        final Clock wallClock = new WallClock();
        final RtpStatistics statistics = new RtpStatistics(wallClock, ssrc);
        final RtpPacket packet1 = new RtpPacket(true, 0, 1, 160 * 1, ssrc, new byte[160]);
        final RtpPacket packet2 = new RtpPacket(true, 0, 2, 160 * 2, ssrc, new byte[160]);
        final RtpPacket packet3 = new RtpPacket(true, 0, 3, 160 * 3, ssrc, new byte[160]);

        // when
        statistics.incomingRtp(packet1);
        statistics.incomingRtp(packet2);
        statistics.incomingRtp(packet3);

        // then
        assertEquals(3, statistics.getRtpPacketsReceived());
        assertEquals(160 * 3, statistics.getRtpOctetsReceived());
    }

    @Test
    public void testRtpSent() {
        // given
        final long ssrc = 12345;
        final Clock wallClock = new WallClock();
        final RtpStatistics statistics = new RtpStatistics(wallClock, ssrc);
        final RtpPacket packet1 = new RtpPacket(true, 0, 1, 160 * 1, ssrc, new byte[160]);
        final RtpPacket packet2 = new RtpPacket(true, 0, 2, 160 * 2, ssrc, new byte[160]);
        final RtpPacket packet3 = new RtpPacket(true, 0, 3, 160 * 3, ssrc, new byte[160]);

        // when
        statistics.outgoingRtp(packet1);
        statistics.outgoingRtp(packet2);
        statistics.outgoingRtp(packet3);

        // then
        assertEquals(3, statistics.getRtpPacketsSent());
        assertEquals(160 * 3, statistics.getRtpOctetsSent());
    }

}
