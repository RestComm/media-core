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
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;

/**
 * Implements jitter buffer.
 * 
 * A jitter buffer temporarily stores arriving packets in order to minimize delay variations. If packets arrive too late then
 * they are discarded. A jitter buffer may be mis-configured and be either too large or too small.
 * 
 * If a jitter buffer is too small then an excessive number of packets may be discarded, which can lead to call quality
 * degradation. If a jitter buffer is too large then the additional delay can lead to conversational difficulty.
 * 
 * A typical jitter buffer configuration is 30mS to 50mS in size. In the case of an adaptive jitter buffer then the maximum size
 * may be set to 100-200mS. Note that if the jitter buffer size exceeds 100mS then the additional delay introduced can lead to
 * conversational difficulty.
 *
 * @author oifa yulian
 */
public class JitterBuffer implements Serializable {

    private static final long serialVersionUID = -389930569631795779L;

    private final static Logger logger = Logger.getLogger(JitterBuffer.class);

    // Buffer properties
    private final DataQueue queue;
    private RtpClock rtpClock;
    private volatile boolean ready;
    private boolean buffering;
    private int maxJitterSize;

    // Listeners
    private BufferListener listener;

    // Jitter Statistics
    private long initialSequenceNr = -1;
    /** packet arrival dead line measured on RTP clock. Initial value equals to infinity. */
    private long arrivalDeadLine = 0;
    private long droppedInRaw = 0;
    private int dropCount;

    // known duration of media currently contained in this buffer.
    private volatile long currentDuration;
    private RTPFormat currentFormat;
    /**
     * Used to calculate network jitter. Measures the relative time it takes for an RTP packet to arrive from the remote server
     * to MMS.
     */
    private long currentTransit = 0;
    /**
     * continuously updated value of network jitter.
     */
    private long currentJitter = 0;

    public JitterBuffer(RtpClock clock, int jitterBufferSize) {
        this.rtpClock = clock;
        this.queue = new DataQueue();
        this.maxJitterSize = jitterBufferSize;
        this.buffering = true;
        this.ready = false;
    }

    public void setMaxJitterSize(int maxJitter) {
        this.maxJitterSize = maxJitter;
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
    public int getDropped() {
        return dropCount;
    }

    /**
     * Gets whether the buffer is in use or if packets are simply forwarded.
     * 
     * @return Returns true if buffer is active; otherwise returns false.
     */
    public boolean isBuffering() {
        return this.buffering;
    }

    /**
     * Sets whether the buffer should be used or if packets should be simply forwarded.
     * 
     * @param useBuffer whether the buffer should be activated
     */
    public void setBuffering(boolean useBuffer) {
        this.buffering = useBuffer;
    }

    /**
     * Assigns listener for this buffer.
     * 
     * @param listener the listener object.
     */
    public void setListener(BufferListener listener) {
        this.listener = listener;
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
        }

        if (this.currentFormat == null || this.currentFormat.getID() != format.getID()) {
            this.currentFormat = format;
            logger.info("Format has been changed: " + this.currentFormat.toString());
        }

        // if this is first packet then synchronize clock
        if (initialSequenceNr == -1) {
            rtpClock.synchronize(packet.getTimestamp());
            initialSequenceNr = packet.getSeqNumber();
            initJitter(packet);
        } else {
            estimateJitter(packet);
        }

        // update clock rate
        rtpClock.setClockRate(this.currentFormat.getClockRate());

        // drop outstanding packets (when the time stamp of arrived packet is less than consumer media time)
        if (packet.getTimestamp() < this.arrivalDeadLine) {
            logger.warn("drop packet: dead line=" + arrivalDeadLine + ", packet time=" + packet.getTimestamp() + ", seq="
                    + packet.getSeqNumber() + ", payload length=" + packet.getPayloadLength() + ", format="
                    + this.currentFormat.toString());
            dropCount++;

            // checking if not dropping too much
            droppedInRaw++;
            if (droppedInRaw == DataQueue.DEFAULT_QUEUE_SIZE / 2 || queue.size() == 0) {
                arrivalDeadLine = 0;
            } else {
                return;
            }
        }

        // Convert the packet to a suitable media frame
        // NOTE: time units of the frame is milliseconds
        Frame f = Memory.allocate(packet.getPayloadLength());
        f.setHeader(null);
        f.setSequenceNumber(packet.getSeqNumber());
        f.setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
        f.setOffset(0);
        f.setLength(packet.getPayloadLength());
        packet.getPayload(f.getData(), 0);
        f.setFormat(this.currentFormat.getFormat());

        droppedInRaw = 0;

        // use time stamp to find correct position to insert a packet
        int currIndex = queue.size() - 1;
        while (currIndex >= 0 && queue.get(currIndex).getTimestamp() > f.getTimestamp()) {
            currIndex--;
        }

        // check for duplicate packet
        if (currIndex >= 0 && queue.get(currIndex).getSequenceNumber() == f.getSequenceNumber()) {
            return;
        }

        queue.add(currIndex + 1, f);

        // recalculate duration of each frame in queue and overall duration
        // since we could insert the frame in the middle of the queue
        currentDuration = 0;
        if (queue.size() > 1) {
            currentDuration = queue.get(queue.size() - 1).getTimestamp() - queue.get(0).getTimestamp();
        }

        for (int i = 0; i < queue.size() - 1; i++) {
            // duration measured by wall clock
            long d = queue.get(i + 1).getTimestamp() - queue.get(i).getTimestamp();
            // in case of RFC2833 event timestamp remains same
            queue.get(i).setDuration(d > 0 ? d : 0);
        }

        // if overall duration is negative we have some mess here!
        if (currentDuration < 0 && queue.size() > 1) {
            logger.warn("Something messy happened. Reseting jitter buffer!");
            reset();
            return;
        }

        // overflow?
        // only now remove packet if overflow, possibly the same packet we just received
        if (queue.size() > DataQueue.DEFAULT_QUEUE_SIZE) {
            logger.warn("Buffer overflow!");
            dropCount++;
            queue.remove(0).recycle();
        }

        // check if this buffer already full
        if (!ready) {
            ready = !buffering || (currentDuration >= maxJitterSize && queue.size() > 1);
            if (ready && listener != null) {
                listener.onFill();
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
        if (queue.isEmpty()) {
            this.ready = false;
            return null;
        }

        // extract packet
        Frame frame = queue.remove(0);

        // buffer empty now? - change ready flag.
        if (queue.size() == 0) {
            this.ready = false;
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
        this.queue.clear();
    }

    public void restart() {
        reset();
        this.ready = false;
        arrivalDeadLine = 0;
        dropCount = 0;
        droppedInRaw = 0;
        currentFormat = null;
        initialSequenceNr = -1;
    }

    private class DataQueue {

        static final int DEFAULT_QUEUE_SIZE = 10;
        private final ArrayList<Frame> queue;

        public DataQueue() {
            this.queue = new ArrayList<Frame>(DEFAULT_QUEUE_SIZE);
        }

        public int size() {
            synchronized (queue) {
                return queue.size();
            }
        }

        public boolean isEmpty() {
            synchronized (queue) {
                return queue.isEmpty();
            }
        }

        public Frame get(int index) {
            synchronized (queue) {
                return this.queue.get(index);
            }
        }

        public Frame remove(int index) {
            synchronized (queue) {
                return this.queue.remove(index);
            }
        }

        public void add(int index, Frame frame) {
            if (frame != null) {
                synchronized (queue) {
                    queue.add(index, frame);
                }
            }
        }

        public void clear() {
            synchronized (queue) {
                while (!queue.isEmpty()) {
                    queue.remove(0).recycle();
                }
            }
        }

    }
}
