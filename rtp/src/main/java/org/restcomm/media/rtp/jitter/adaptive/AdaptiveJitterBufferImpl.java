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

package org.restcomm.media.rtp.jitter.adaptive;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.BufferListener;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.adaptive.strategy.PlayoutStrategy;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.spi.memory.Frame;
import org.restcomm.media.spi.memory.Memory;

/**
 * Implements adaptive jitter buffer.
 * 
 * @author jehanzeb qayyum
 */
public class AdaptiveJitterBufferImpl implements AdaptiveJitterBuffer, Serializable {

    private static final int DEFAULT_INITIAL_CAPACITY = 100;

    public static class FrameComparator implements Comparator<Frame> {
        @Override
        public int compare(Frame f0, Frame f1) {
            if (f0.getSequenceNumber() == f1.getSequenceNumber())
                return 0;
            if (f0.getTimestamp() <= f1.getTimestamp())
                return -1;
            if (f0.getTimestamp() > f1.getTimestamp())
                return 1;
            return -1;
        }
    }

    private static final long serialVersionUID = 183012984750647142L;

    private Timer playoutTimer = new Timer("PlayoutTimer", true);

    // strategy for frames playout
    private PlayoutStrategy playoutStrategy;

    // the underlying buffer
    private PriorityQueue<Frame> queue = new PriorityQueue<Frame>(DEFAULT_INITIAL_CAPACITY, new FrameComparator());

    // RTP clock
    private RtpClock rtpClock;

    // first received sequence number
    private volatile long isn = -1;

    // packet arrival dead line measured on RTP clock.
    // initial value equals to infinity
    private volatile long arrivalDeadLine = 0;

    // The number of dropped packets
    // private volatile int dropCount;

    // known duration of media wich contains in this buffer.
    // private volatile long bufferDuration;

    // buffer's monitor
    private BufferListener bufferListener;

    /**
     * used to calculate network jitter. currentTransit measures the relative time it takes for an RTP packet to arrive from the
     * remote server to MMS
     */
    private long currentTransit = 0;

    /**
     * continuously updated value of network jitter
     */
    private long currentJitter = 0;

    private Boolean useBuffer = true;

    private final static Logger logger = Logger.getLogger(AdaptiveJitterBufferImpl.class);

    private final Lock lock = new ReentrantLock();

    public AdaptiveJitterBufferImpl() {
    }

    private void initJitter(RtpPacket firstPacket) {
        long arrival = rtpClock.getLocalRtpTime();
        long firstPacketTimestamp = firstPacket.getTimestamp();
        currentTransit = arrival - firstPacketTimestamp;
    }

    /**
     * Calculates the current network jitter, which is an estimate of the statistical variance of the RTP data packet
     * interarrival time: http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    private void estimateJitter(RtpPacket newPacket) {
        long arrival = rtpClock.getLocalRtpTime();
        long newPacketTimestamp = newPacket.getTimestamp();
        long transit = arrival - newPacketTimestamp;
        long d = transit - currentTransit;
        if (d < 0) {
            d = -d;
        }
        // logger.info(String.format("recalculating jitter: arrival=%d, newPacketTimestamp=%d, transit=%d, transit delta=%d",
        // arrival, newPacketTimestamp, transit, d ));
        currentTransit = transit;
        currentJitter += d - ((currentJitter + 8) >> 4);
    }

    /**
     * 
     * @return the current value of the network RTP jitter. The value is in normalized form as specified in RFC 3550
     *         http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    public long getEstimatedJitter() {
        long jitterEstimate = currentJitter >> 4;
        // logger.info(String.format("Jitter estimated at %d. Current transit time is %d.", jitterEstimate, currentTransit));
        return jitterEstimate;
    }

    /**
     * Gets the interarrival jitter.
     *
     * @return the current jitter value.
     */
    public double getJitter() {
        return 0;
    }

    /**
     * Gets the maximum interarrival jitter.
     *
     * @return the jitter value.
     */
    public double getMaxJitter() {
        return 0;
    }

    /**
     * Get the number of dropped packets.
     * 
     * @return the number of dropped packets.
     */
    /*
     * public int getDropped() { return dropCount; }
     */

    public boolean bufferInUse() {
        return this.useBuffer;
    }

    @Override
    public void setInUse(boolean useBuffer) {
        this.useBuffer = useBuffer;
    }

    private void safeWrite(RtpPacket packet, RTPFormat format) {
        // if this is first packet then synchronize clock
        if (isn == -1) {
            rtpClock.synchronize(packet.getTimestamp());
            isn = packet.getSeqNumber();
            initJitter(packet);
        } else {
            estimateJitter(packet);
        }

        // update clock rate
        rtpClock.setClockRate(format.getClockRate());

        if (isLateArrival(packet)) {
            // dropCount++;
            return;
        }

        Frame f = createFrame(packet, format);
        if (queue.contains(f)) { // duplicate
            f.recycle();
            return;
        }

        queue.add(f);

        // recalculate duration of all packets since we may insert in the middle
        recalculateDurations(f);

        // this.bufferDuration = queue.size() > 1 ? (queue.last().getTimestamp() - queue.first().getTimestamp()) : 0;
        // if overall duration is negative we have some mess here,try to
        // reset
        // if (this.bufferDuration < 0 && queue.size() > 1) {
        // logger.warn("Something messy happened. Reseting jitter buffer!");
        // reset();
        // return;
        // }

        // wakeup listener
        if (bufferListener != null) {
            if (!useBuffer) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Jitter Buffer is ready! [frames=" + queue.size() + "]");
                }
                bufferListener.onFill();
            } else {
                schedulePlayout(f, packet);
            }
        }

    }

    private void schedulePlayout(Frame f, RtpPacket packet) {
        if (this.playoutStrategy == null) {
            return;
        }

        long playoutOffset = this.playoutStrategy.getPlayoutOffset(packet);
        if (playoutOffset >= 0) {
            f.setPlayoutOffset(playoutOffset);
            try {
                playoutTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Jitter Buffer is ready to be played! [frames=" + queue.size() + "]");
                        }
                        bufferListener.onFill();
                    }
                }, playoutOffset);
                if (logger.isDebugEnabled()) {
                    logger.debug(
                            "Jitter Buffer is scheuled! [playoutOfgset = " + playoutOffset + " frames=" + queue.size() + "]");
                }
            } catch (Exception e) {
                logger.error("Could not schedule playout task.", e);
            }
        }
    }

    private void recalculateDurations(Frame f) {
        Frame curr, next;
        Iterator<Frame> iter = queue.iterator();
        while (iter.hasNext()) {
            curr = iter.next();
            if (iter.hasNext()) {
                next = iter.next();
                // duration measured by wall clock
                long duration = next.getTimestamp() - curr.getTimestamp();
                // in case of RFC2833 event timestamp remains same
                curr.setDuration(duration > 0 ? duration : 0);
            }
        }
    }

    private Frame createFrame(RtpPacket packet, RTPFormat format) {
        Frame f = Memory.allocate(packet.getPayloadLength());
        // put packet into buffer irrespective of its sequence number
        f.setHeader(null);
        f.setSequenceNumber(packet.getSeqNumber());
        // here time is in milliseconds
        f.setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
        f.setOffset(0);
        f.setLength(packet.getPayloadLength());
        packet.getPayload(f.getData(), 0);
        // set format
        f.setFormat(format.getFormat());
        return f;
    }

    private boolean isLateArrival(RtpPacket packet) {
        // drop outstanding packets
        // packet is outstanding if its timestamp of arrived packet is less
        // then consumer media time
        if (packet.getTimestamp() < this.arrivalDeadLine) {
            if (logger.isTraceEnabled()) {
                logger.trace("drop packet: dead line=" + arrivalDeadLine + ", packet time=" + packet.getTimestamp() + ", seq="
                        + packet.getSeqNumber() + ", payload length=" + packet.getPayloadLength());
            }
            return true;
        }
        return false;
    }

    /**
     * Accepts specified packet
     *
     * @param packet the packet to accept
     */
    @Override
    public void write(RtpPacket packet, RTPFormat format) {
        // checking format
        if (format == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No format specified. Packet dropped!");
            }
            return;
        }

        boolean locked = false;
        try {
            locked = this.lock.tryLock() || this.lock.tryLock(5, TimeUnit.MILLISECONDS);
            if (locked) {
                safeWrite(packet, format);
            }
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Could not aquire write lock for jitter buffer. Dropped packet.");
            }
        } finally {
            if (locked) {
                this.lock.unlock();
            }
        }
    }

    /**
     * Polls packet from buffer's head.
     *
     * @param timestamp the media time measured by reader
     * @return the media frame.
     */
    public Frame read(long timestamp) {
        Frame frame = null;
        boolean locked = false;
        try {
            locked = this.lock.tryLock() || this.lock.tryLock(5, TimeUnit.MILLISECONDS);
            if (locked) {
                frame = safeRead();
            }
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Could not acquire reading lock for jitter buffer.");
            }
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
        return frame;
    }

    private Frame safeRead() {
        if (queue.size() == 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("Jitter Buffer is empty. Consumer will wait until buffer is filled.");
            }
            return null;
        }

        // queue head
        Frame frame = queue.peek();

        if (frame.getPlayoutOffset() != -1
                && (frame.getTimestamp() + frame.getPlayoutOffset()) < rtpClock.getWallClock().getCurrentTime()) {
            if (logger.isTraceEnabled()) {
                logger.trace("Cannot read jitter buffer further, since playout time did not come yet.");
            }
            return null;
        }

        // remove head
        queue.poll();

        // buffer empty now? - change ready flag.
        if (queue.size() == 0) {
            if (logger.isTraceEnabled()) {
                logger.trace("Read last packet from Jitter Buffer.");
            }
            // arrivalDeadLine = 0;
            // set it as 1 ms since otherwise will be dropped by pipe
            frame.setDuration(1);
        }

        arrivalDeadLine = rtpClock.convertToRtpTime(frame.getTimestamp() + frame.getDuration());

        // convert duration to nanoseconds
        frame.setDuration(frame.getDuration() * 1000000L);
        frame.setTimestamp(frame.getTimestamp() * 1000000L);

        return frame;
    }

    /**
     * Resets buffer.
     */
    public void reset() {
        boolean locked = false;
        try {
            locked = lock.tryLock() || lock.tryLock(5, TimeUnit.MILLISECONDS);
            if (locked) {
                while (queue.size() > 0) {
                    queue.poll().recycle();
                }
                playoutTimer.purge();
            }
        } catch (InterruptedException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("Could not acquire lock to reset jitter buffer.");
            }
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    public void restart() {
        reset();
        arrivalDeadLine = 0;
        // dropCount = 0;
        isn = -1;

        if (logger.isDebugEnabled()) {
            logger.debug("Restarted jitter buffer.");
        }
    }

    @Override
    public void setListener(BufferListener listener) {
        this.bufferListener = listener;
    }

    public long getCurrentDelay() {
        return this.currentTransit;
    }

    /*
     * public long getBufferDuration() { return bufferDuration; }
     */

    public void setPlayoutStrategy(PlayoutStrategy playoutStrategy) {
        this.playoutStrategy = playoutStrategy;
    }

    @Override
    public String toString() {
        return "AdaptiveJitterBuffer [isn=" + isn + ", arrivalDeadLine=" + arrivalDeadLine + ", currentTransit="
                + currentTransit + ", currentJitter=" + currentJitter + ", useBuffer=" + useBuffer + "]";
    }

    @Override
    public void setRtpClock(RtpClock rtpClock) {
        this.rtpClock = rtpClock;
    }

    @Override
    public void setJitterbufferSize(int jitterBufferSize) {
    }

}
