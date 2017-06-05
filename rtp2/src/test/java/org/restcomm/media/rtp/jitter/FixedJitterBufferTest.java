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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.spi.memory.Frame;

/**
 * @author kulikov
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class FixedJitterBufferTest {

    private MockWallClock wallClock = new MockWallClock();
    private RtpClock rtpClock = new RtpClock(wallClock);

    // private int period = 20;
    private int jitter = 40;

    private FixedJitterBuffer jitterBuffer = new FixedJitterBuffer(rtpClock, jitter);

    @Before
    public void setUp() {
        rtpClock.setClockRate(8000);
        jitterBuffer.reset();
    }

    @Test
    public void testNormalReadWrite() throws Exception {
        RtpPacket[] stream = createStream(100);

        Frame[] media = new Frame[stream.length];
        for (int i = 0; i < stream.length; i++) {
            wallClock.tick(20000000L);
            jitterBuffer.write(stream[i], AVProfile.audio.find(8));
            media[i] = jitterBuffer.read(wallClock.getTime());
        }

        this.checkSequence(media);
        assertEquals(0, 0);
    }

    @Test
    public void testInnerSort() throws Exception {
        // given
        RtpPacket p1 = new RtpPacket(false, 8, 1, 160 * 1, 123, new byte[160]);
        RtpPacket p2 = new RtpPacket(false, 8, 2, 160 * 2, 123, new byte[160]);
        RtpPacket p3 = new RtpPacket(false, 8, 3, 160 * 3, 123, new byte[160]);
        RtpPacket p4 = new RtpPacket(false, 8, 4, 160 * 4, 123, new byte[160]);

        // when
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        jitterBuffer.write(p2, AVProfile.audio.find(8));
        jitterBuffer.write(p4, AVProfile.audio.find(8));
        jitterBuffer.write(p3, AVProfile.audio.find(8));

        // then
        Frame frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(1, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(2, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(3, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(4, frame.getSequenceNumber());
    }

    @Test
    public void testOutstanding() throws Exception {
        // given
        RtpPacket p1 = new RtpPacket(false, 8, 1, 160 * 1, 123, new byte[160]);
        RtpPacket p2 = new RtpPacket(false, 8, 2, 160 * 2, 123, new byte[160]);
        RtpPacket p3 = new RtpPacket(false, 8, 3, 160 * 3, 123, new byte[160]);
        RtpPacket p4 = new RtpPacket(false, 8, 5, 160 * 5, 123, new byte[160]);

        // when
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        jitterBuffer.write(p3, AVProfile.audio.find(8));
        jitterBuffer.write(p4, AVProfile.audio.find(8));

        // then
        assertEquals(0, jitterBuffer.getDropped());

        wallClock.tick(100000000L); // 60ms + 40ms

        Frame buffer = jitterBuffer.read(wallClock.getTime());
        assertEquals(1, buffer.getSequenceNumber());

        buffer = jitterBuffer.read(wallClock.getTime());
        assertEquals(3, buffer.getSequenceNumber());

        // when
        jitterBuffer.write(p2, AVProfile.audio.find(8));

        // then
        assertEquals(1, jitterBuffer.getDropped());
    }

    @Test
    public void testEmpty() throws Exception {
        // given
        RtpPacket p1 = new RtpPacket(false, 8, 1, 160 * 1, 123, new byte[160]);
        RtpPacket p2 = new RtpPacket(false, 8, 2, 160 * 2, 123, new byte[160]);
        RtpPacket p3 = new RtpPacket(false, 8, 3, 160 * 3, 123, new byte[160]);

        // when
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        jitterBuffer.write(p2, AVProfile.audio.find(8));
        jitterBuffer.write(p3, AVProfile.audio.find(8));

        // then
        Frame frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(1, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(2, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(3, frame.getSequenceNumber());

        frame = jitterBuffer.read(wallClock.getTime());
        assertEquals(null, frame);
    }

    @Test
    public void testOverflow() {
        RtpPacket[] stream = createStream(5);
        for (int i = 0; i < stream.length; i++) {
            jitterBuffer.write(stream[i], AVProfile.audio.find(8));
        }

        Frame data = jitterBuffer.read(wallClock.getTime());
        assertEquals(1, data.getSequenceNumber());
    }

    /**
     * 
     * Test that network jitter for RTP packets is estimated correctly
     * 
     * http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    @Test
    public void testJitter() {
        // given
        // the time stamp for each packet increases by 10ms=160 time stamp units for sampling rate 8KHz
        RtpPacket p1 = new RtpPacket(false, 8, 1, 160 * 1, 123, new byte[160]);
        RtpPacket p2 = new RtpPacket(false, 8, 2, 160 * 2, 123, new byte[160]);
        RtpPacket p3 = new RtpPacket(false, 8, 2, 160 * 3, 123, new byte[160]);
        RtpPacket p4 = new RtpPacket(false, 8, 3, 160 * 4, 123, new byte[160]);
        RtpPacket p5 = new RtpPacket(false, 8, 3, 160 * 5, 123, new byte[160]);

        long jitterDeltaLimit = 1; // 1 sampling units delta for timing and rounding errors , i.e. 1/8ms

        // write first packet, expected jitter = 0
        jitterBuffer.write(p1, AVProfile.audio.find(8));
        assertEquals(0, jitterBuffer.getEstimatedJitter(), jitterDeltaLimit);

        // move time forward by 20ms and write the second packet
        // the transit time should remain approximately the same - near 0ms.
        // expected jitter = 0;
        wallClock.tick(20000000L);
        jitterBuffer.write(p2, AVProfile.audio.find(8));
        assertEquals(0, jitterBuffer.getEstimatedJitter(), jitterDeltaLimit);

        // move time forward by 30ms and write the next packet
        // the transit time should increase by 10ms,
        // as suggested by the difference in the third packet timestamp (160*3) and the 20ms delay for the server to receive the
        // second packet
        // expected jitter should be close to the 10ms delay in timestamp units/16, i.e. 80/16.
        wallClock.tick(30000000L);
        jitterBuffer.write(p3, AVProfile.audio.find(8));
        assertEquals(5, jitterBuffer.getEstimatedJitter(), jitterDeltaLimit);

        // move time forward by 20ms and write the next packet
        // the transit time does not change from the previous packet.
        // The jitter should stay approximately the same.
        wallClock.tick(20000000L);
        jitterBuffer.write(p4, AVProfile.audio.find(8));
        assertEquals(4, jitterBuffer.getEstimatedJitter(), jitterDeltaLimit);

        // move time forward by 30ms and write the next packet
        // packet was delayed 10ms again.
        // The estimated jitter should increase significantly, by nearly 5ms (80/16)
        wallClock.tick(30000000L);
        jitterBuffer.write(p5, AVProfile.audio.find(8));
        assertEquals(9, jitterBuffer.getEstimatedJitter(), jitterDeltaLimit);

    }

    private RtpPacket[] createStream(int size) {
        RtpPacket[] stream = new RtpPacket[size];
        int it = 12345;

        for (int i = 0; i < stream.length; i++) {
            stream[i] = new RtpPacket(false, 8, i + 1, 160 * (i + 1) + it, 123, new byte[160]);
        }
        return stream;
    }

    private void checkSequence(Frame[] media) throws Exception {
        boolean res = true;
        for (int i = 0; i < media.length - 1; i++) {
            if (media[i] == null) {
                throw new Exception("Null data at position: " + i);
            }

            if (media[i + 1] == null) {
                throw new Exception("Null data at position: " + (i + 1));
            }

            res &= (media[i + 1].getSequenceNumber() - media[i].getSequenceNumber() == 1);
        }

        assertTrue("Wrong sequence ", res);
    }

    private class MockWallClock implements Clock {

        private TimeUnit unit = TimeUnit.NANOSECONDS;
        private long time = System.nanoTime();
        private long currTime = System.currentTimeMillis();

        public long getTime() {
            return time;
        }

        public long getTime(TimeUnit timeUnit) {
            return timeUnit.convert(time, unit);
        }

        public TimeUnit getTimeUnit() {
            return unit;
        }

        public void tick(long amount) {
            time += amount;
            currTime += TimeUnit.NANOSECONDS.toMillis(amount);
        }

        public long getCurrentTime() {
            return currTime;
        }
    }

}
