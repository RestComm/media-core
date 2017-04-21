/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.restcomm.media.rtp.jitter.adaptive.strategy;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.rtp.BufferListener;
import org.restcomm.media.rtp.MockWallClock;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.adaptive.AdaptiveJitterBuffer;
import org.restcomm.media.sdp.format.AVProfile;

public class LinearRecursiveFilterTest {
    private MockWallClock clock = new MockWallClock();
    private RtpClock rtpClock = new RtpClock(clock);

    private AdaptiveJitterBuffer jitterBuffer = new AdaptiveJitterBuffer(rtpClock);
    private LinearRecursiveFilter linearRecursiveFilter = org.mockito.Mockito.spy(LinearRecursiveFilter.class);

    private final static Logger logger = Logger.getLogger(LinearRecursiveFilterTest.class);

    public LinearRecursiveFilterTest() {
        jitterBuffer.setPlayoutStrategy(linearRecursiveFilter);
        jitterBuffer.setListener(new BufferListener() {
            @Override
            public void onFill() {
                logger.debug("onFill>>");
                // jitterBuffer.read(0);
            }
        });
        linearRecursiveFilter.jitterBuffer = jitterBuffer;
    }

    @Before
    public void setUp() {
        rtpClock.setClockRate(8000);
        jitterBuffer.reset();
    }

    @Test
    public void testNoDelay() throws Exception {
        RtpPacket p1 = new RtpPacket(172, false);
        p1.wrap(true, 8, 1, 160 * 0, 123, new byte[160], 0, 160);

        RtpPacket p2 = new RtpPacket(172, false);
        p2.wrap(false, 8, 2, 160 * 1, 123, new byte[160], 0, 160);

        RtpPacket p3 = new RtpPacket(172, false);
        p3.wrap(true, 8, 3, 160 * 2, 123, new byte[160], 0, 160);

        logger.debug("packet 1");
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(0, linearRecursiveFilter.playoutOffset);

        clock.tick(160 / 8 * 1000000);

        logger.debug("packet 2");
        jitterBuffer.write(p2, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(-1, linearRecursiveFilter.playoutOffset);

        clock.tick(160 / 8 * 1000000);

        logger.debug("packet 3");
        jitterBuffer.write(p3, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(0, linearRecursiveFilter.playoutOffset);
    }

    @Test
    public void testWithDelay() throws Exception {
        RtpPacket p1 = new RtpPacket(172, false);
        p1.wrap(true, 8, 1, 160 * 0, 123, new byte[160], 0, 160);

        RtpPacket p2 = new RtpPacket(172, false);
        p2.wrap(false, 8, 2, 160 * 1, 123, new byte[160], 0, 160);

        RtpPacket p3 = new RtpPacket(172, false);
        p3.wrap(true, 8, 3, 160 * 2, 123, new byte[160], 0, 160);

        logger.debug("packet 1");
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(0, linearRecursiveFilter.playoutOffset);

        clock.tick((160 + 8) / 8 * 1000000);

        logger.debug("packet 2");
        jitterBuffer.write(p2, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(-1, linearRecursiveFilter.playoutOffset);

        clock.tick((160 + 8) / 8 * 1000000);

        logger.debug("packet 3");
        jitterBuffer.write(p3, AVProfile.audio.find(8));
        logger.debug(linearRecursiveFilter);
        assertEquals(0, linearRecursiveFilter.playoutOffset);
    }
}
