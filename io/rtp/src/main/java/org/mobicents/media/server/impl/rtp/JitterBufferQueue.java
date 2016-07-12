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
package org.mobicents.media.server.impl.rtp;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.mobicents.media.server.io.sdp.format.RTPFormat;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements jitter buffer.
 *
 * A jitter buffer temporarily stores arriving packets in order to minimize
 * delay variations. If packets arrive too late then they are discarded. A
 * jitter buffer may be mis-configured and be either too large or too small.
 *
 * If a jitter buffer is too small then an excessive number of packets may be
 * discarded, which can lead to call quality degradation. If a jitter buffer is
 * too large then the additional delay can lead to conversational difficulty.
 *
 * A typical jitter buffer configuration is 30mS to 50mS in size. In the case of
 * an adaptive jitter buffer then the maximum size may be set to 100-200mS. Note
 * that if the jitter buffer size exceeds 100mS then the additional delay
 * introduced can lead to conversational difficulty.
 *
 * @author oifa yulian
 */
public class JitterBufferQueue implements Serializable, Comparator<Frame> {

    private static final long serialVersionUID = -389930569631795779L;

    //The underlying buffer size
    private static final int QUEUE_SIZE = 100;
    //the underlying buffer
    private PriorityQueue<Frame> queue = new PriorityQueue<Frame>(QUEUE_SIZE, this);

    //RTP clock
    private RtpClock rtpClock;
    //first received sequence number
    private long isn = -1;

    //allowed jitter
    private long jitterBufferSize;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long arrivalDeadLine = 0;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long droppedInRaw = 0;

    //The number of dropped packets
    private int dropCount;

    //known duration of media wich contains in this buffer.
    private volatile long duration;

    //buffer's monitor
    private BufferListener listener;

    private volatile AtomicBoolean ready = new AtomicBoolean(false);

    /**
     * used to calculate network jitter. currentTransit measures the relative
     * time it takes for an RTP packet to arrive from the remote server to MMS
     */
    private long currentTransit = 0;

    /**
     * continuously updated value of network jitter
     */
    private long currentJitter = 0;

    //transmission formats
    private RTPFormats rtpFormats = new RTPFormats();

    //currently used format
    private RTPFormat format;

    private Boolean useBuffer = true;

    private final static Logger logger = Logger.getLogger(JitterBufferQueue.class);

    /**
     * Creates new instance of jitter.
     *
     * @param clock the rtp clock.
     */
    public JitterBufferQueue(RtpClock clock, int jitterBufferSize) {
        this.rtpClock = clock;
        this.jitterBufferSize = jitterBufferSize;
    }

    private void initJitter(RtpPacket firstPacket) {
        long arrival = rtpClock.getLocalRtpTime();
        long firstPacketTimestamp = firstPacket.getTimestamp();
        currentTransit = arrival - firstPacketTimestamp;
    }

    /**
     * Calculates the current network jitter, which is an estimate of the
     * statistical variance of the RTP data packet interarrival time:
     * http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    private void estimateJitter(RtpPacket newPacket) {
        long arrival = rtpClock.getLocalRtpTime();
        long newPacketTimestamp = newPacket.getTimestamp();
        long transit = arrival - newPacketTimestamp;
        long d = transit - currentTransit;
        if (d < 0) {
            d = -d;
        }
        //logger.info(String.format("recalculating jitter: arrival=%d, newPacketTimestamp=%d, transit=%d, transit delta=%d", arrival, newPacketTimestamp, transit, d ));
        currentTransit = transit;
        //use >>> as java >> fills with sign bit
        currentJitter += d - ((currentJitter + 8) >>> 4);
    }

    /**
     *
     * @return the current value of the network RTP jitter. The value is in
     * normalized form as specified in RFC 3550
     * http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    public long getEstimatedJitter() {
        //use >>> as java >> fills with sign bit
        long jitterEstimate = currentJitter >>> 4;
        // logger.info(String.format("Jitter estimated at %d. Current transit time is %d.", jitterEstimate, currentTransit));
        return jitterEstimate;
    }

    public void setFormats(RTPFormats rtpFormats) {
        this.rtpFormats = rtpFormats;
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
    public int getDropped() {
        return dropCount;
    }

    public boolean bufferInUse() {
        return this.useBuffer;
    }

    public void setBufferInUse(boolean useBuffer) {
        this.useBuffer = useBuffer;
    }

    /**
     * Assigns listener for this buffer.
     *
     * @param listener the listener object.
     */
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }

    private Frame allocateFrame(RtpPacket packet) {
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
        f.setFormat(this.format.getFormat());
        return f;
    }

    private boolean hasToDrop(RtpPacket packet) {
        boolean toDrop = false;

        if (packet.getTimestamp() < this.arrivalDeadLine) {
            logger.warn("drop packet: dead line=" + arrivalDeadLine + ", packet time=" + packet.getTimestamp() + ", seq=" + packet.getSeqNumber() + ", payload length=" + packet.getPayloadLength() + ", format=" + this.format.toString());
            dropCount++;

            // checking if not dropping too much
            droppedInRaw++;
            if (droppedInRaw == QUEUE_SIZE / 2 || queue.size() == 0) {
                arrivalDeadLine = 0;
            } else {
                toDrop = true;
            }
        }
        return toDrop;
    }

    private void syncClock(RtpPacket packet) {
        // if this is first packet then synchronize clock
        if (isn == -1) {
            rtpClock.synchronize(packet.getTimestamp());
            isn = packet.getSeqNumber();
            initJitter(packet);
        } else {
            estimateJitter(packet);
        }
    }

    private void recalculateDuration(Frame f) {
        // recalculate duration of each frame in queue and overall duration
        // since we could insert the frame in the middle of the queue
        duration = 0;
        if (queue.size() > 1) {
            duration = f.getTimestamp() - queue.peek().getTimestamp();
        }

        Iterator<Frame> iterator = queue.iterator();
        Frame previousFrame = null;
        while (iterator.hasNext()) {
            Frame currentFrame = iterator.next();
            if (previousFrame != null) {
                // duration measured by wall clock
                long d = currentFrame.getTimestamp() - previousFrame.getTimestamp();
                // in case of RFC2833 event timestamp remains same
                currentFrame.setDuration(d > 0 ? d : 0);
            }
            previousFrame = currentFrame;
        }
    }

    private void checkBufferFull() {
        // check if this buffer already full
        if (!ready.get()) {
            ready.set(!useBuffer || (duration >= jitterBufferSize && queue.size() > 1));
            if (ready.get() && listener != null) {
                listener.onFill();
            }
        }
    }

    private void checkOverflow() {
        // overflow?
        // only now remove packet if overflow , possibly the same packet we just received
        if (queue.size() > QUEUE_SIZE) {
            logger.warn("Buffer overflow!");
            dropCount++;
            queue.poll().recycle();
        }
    }

    private void updateFormat(RTPFormat format) {
        if (this.format == null || this.format.getID() != format.getID()) {
            this.format = format;
            // update clock rate
            rtpClock.setClockRate(this.format.getClockRate());
            logger.info("Format has been changed: " + this.format.toString());
        }
    }

    /**
     * Accepts specified packet
     *
     * @param packet the packet to accept
     */
    public void write(RtpPacket packet, RTPFormat format) {
        // checking format
        if (format == null) {
            logger.warn("No format specified. Packet dropped!");
            return;
        } else {
            updateFormat(format);
        }

        syncClock(packet);

        // drop outstanding packets
        // packet is outstanding if its timestamp of arrived packet is less
        // then consumer media time
        if (hasToDrop(packet)) {
            return;
        }

        Frame f = allocateFrame(packet);
        // make checks only if have packet
        if (f != null) {

            // check for duplicate packet
            if (queue.contains(f)) {
                return;
            }

            queue.offer(f);
            //restart drop count
            droppedInRaw = 0;

            recalculateDuration(f);

            // if overall duration is negative we have some mess here,try to
            // reset
            if (duration < 0 && queue.size() > 1) {
                logger.warn("Something messy happened. Reseting jitter buffer!");
                reset();
                return;
            }

            checkOverflow();

            checkBufferFull();
        }
    }

    /**
     * Polls packet from buffer's head.
     *
     * @param timestamp the media time measured by reader
     * @return the media frame.
     */
    public Frame read(long timestamp) {
        if (queue.size() == 0) {
            this.ready.set(false);
            return null;
        }

        //extract packet
        Frame frame = queue.poll();

        //buffer empty now? - change ready flag.
        if (queue.size() == 0) {
            this.ready.set(false);
            //arrivalDeadLine = 0;
            //set it as 1 ms since otherwise will be dropped by pipe
            frame.setDuration(1);
        }

        arrivalDeadLine = rtpClock.convertToRtpTime(frame.getTimestamp() + frame.getDuration());

        //convert duration to nanoseconds
        frame.setDuration(frame.getDuration() * 1000000L);
        frame.setTimestamp(frame.getTimestamp() * 1000000L);

        return frame;
    }

    /**
     * Resets buffer.
     */
    public void reset() {
        while (queue.size() > 0) {
            queue.poll().recycle();
        }
    }

    public void restart() {
        reset();
        this.ready.set(false);
        arrivalDeadLine = 0;
        dropCount = 0;
        droppedInRaw = 0;
        format = null;
        isn = -1;
    }

    @Override
    public int compare(Frame o1, Frame o2) {
        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }

    public long getCurrentTransit() {
        return currentTransit;
    }

    public long getCurrentJitter() {
        return currentJitter;
    }
    
    
}
