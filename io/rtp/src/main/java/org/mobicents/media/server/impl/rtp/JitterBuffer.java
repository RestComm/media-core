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
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfConverter;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.memory.Memory;
import org.mobicents.media.server.utils.Text;

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
 * @author kulikov
 */
public class JitterBuffer implements Serializable {
    private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
    static {
        dtmf.setOptions(new Text("0-15"));
    }
    //The underlying buffer size
    private static final int QUEUE_SIZE = 10;
    //the underlying buffer
    private Frame[] queue = new Frame[QUEUE_SIZE];

    //read and write cursors
    private int readCursor;
    private int writeCursor = -1;

    //the actual length of the queue
    private volatile int len = -1;

    //RTP clock
    private RtpClock rtpClock;
    //first received sequence number
    private long isn = -1;

    //allowed jitter
    private long jitter;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long arrivalDeadLine = 0;

    private long r, s;
    private double j, jm;

    //The number of dropped packets
    private int dropCount;

    //known duration of media wich contains in this buffer.
    private volatile long duration;

    //buffer's monitor
    private BufferListener listener;

    private volatile boolean ready;
    
    //transmission formats
    private RTPFormats rtpFormats = new RTPFormats();
    
    //currently used format
    private RTPFormat format;
    
    //RTP dtmf event converter
    private DtmfConverter dtmfConverter;
    
    private final Object LOCK = new Object();
    /**
     * Creates new instance of jitter.
     * 
     * @param clock the rtp clock.
     */
    public JitterBuffer(RtpClock clock, int jitter) {
        this.rtpClock = clock;
        this.jitter = rtpClock.convertToRtpTime(jitter);
        this.dtmfConverter = new DtmfConverter();
        this.dtmfConverter.setClock(clock);
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
        return j;
    }

    /**
     * Gets the maximum interarrival jitter.
     *
     * @return the jitter value.
     */
    public double getMaxJitter() {
        return jm;
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
    public void write(RtpPacket packet) {
    	synchronized(LOCK) {
    		//if this is first packet then synchronize clock
    		if (isn == -1) {
    			rtpClock.synchronize(packet.getTimestamp());
    			isn = packet.getSeqNumber();
    		}

    		//checking format
    		if (this.format == null) {
    			//if format is not known yet assign the format of this packet
    			this.format = rtpFormats.find(packet.getPayloadType());
    			System.out.println("Format has been changed: " + this.format.toString());
    		} else if (this.format.getID() != packet.getPayloadType()) {
    			//format has been changed 
    			this.format = rtpFormats.find(packet.getPayloadType());
    			System.out.println("Format has been changed: " + this.format.toString());
    		}

    		//ignore unknow packet
    		if (this.format == null) {
    			//unknown packet
    			return;
    		}
        
    		//update clock rate
    		rtpClock.setClockRate(this.format.getClockRate());
        
    		this.updateWritePosition();

    		//overflow?
    		//drop packet from the head
    		if (len == queue.length) {
//          	  System.out.println("Buffer overflow");
    			dropCount++;
    			this.updateReadPosition();
    		}
        
        
    		if (this.format != null && this.format.getFormat().matches(dtmf)) {
    			Frame f = dtmfConverter.process(packet);
    			if (f != null) {
    				queue[writeCursor] = f;
    				queue[writeCursor].setSequenceNumber(packet.getSeqNumber());
    				queue[writeCursor].setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
    			}
    		} else {
    			//drop outstanding packets
    			//packet is outstanding if its timestamp of arrived packet is less
    			//then consumer media time
    			if (packet.getTimestamp() < this.arrivalDeadLine) {
    				System.out.println("drop packet: dead line=" + arrivalDeadLine
                        + ", packet time=" + packet.getTimestamp() + ", seq=" + packet.getSeqNumber()
                        + ", payload length=" + packet.getPayloadLength());
    				dropCount++;
    				return;
    			}
    			//put packet into buffer irrespective of its sequence number
    			queue[writeCursor] = Memory.allocate(packet.getPayloadLength());
    			queue[writeCursor].setHeader(null);
    			queue[writeCursor].setSequenceNumber(packet.getSeqNumber());
    			//here time is in milliseconds
    			queue[writeCursor].setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
    			queue[writeCursor].setOffset(0);
    			queue[writeCursor].setLength(packet.getPayloadLength());
    			packet.getPyalod(queue[writeCursor].getData(), 0);

    			//set format
    			queue[writeCursor].setFormat(this.format.getFormat());
    		}
    		//we are expecting that sequence number still grow, if not
    		//move packet forward direction till its sequence number remains
    		//less then sequence number of previous
    		sort(writeCursor);

    		//update duration of the previous packet;
    		//if previous packet exists then len greater then 0
    		if (len > 0) {
    			int p = dec(writeCursor);
    			//duration measured by wall clock
    			long d = queue[writeCursor].getTimestamp() - queue[p].getTimestamp();
            
    			//in case of RFC2833 event timestamp remains same
    			if (d > 0) {
    				queue[p].setDuration(d);
    			}
    			duration += queue[p].getDuration();
    		}

    		//compute interarrival jitter.
    		//@see RFC1889
    		j += (double)(Math.abs((r - s) - (rtpClock.getTime() - packet.getTimestamp())) - j)/16D;
    		r = rtpClock.getTime();
    		s = packet.getTimestamp();

    		//find max jitter value
    		if (j > jm) jm = j;
        
    		//check if this buffer already full
    		if (!ready) {
    			ready = duration >= jitter && len > 1;
    			if (ready) {
    				if (listener != null) {
    					listener.onFill();
    				}
    			}
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
    	synchronized(LOCK) {
    		if (len < 0) {
    			return null;
    		}

    		//extract packet
    		Frame frame = queue[readCursor];
    		queue[readCursor] = null;
    		this.updateReadPosition();

    		//buffer empty now? - change ready flag.
    		if (len < 0) {
    			this.ready = false;
    			arrivalDeadLine = 0;
    			frame.setDuration(0);
    		} else {
    			long d = this.arrivalDeadLine;
    			arrivalDeadLine = rtpClock.convertToRtpTime(frame.getTimestamp() + frame.getDuration());
    		}

    		//update buffer duration
    		duration = ready ? duration - frame.getDuration() : 0;

    		//convert duration to nanoseconds
    		frame.setDuration(frame.getDuration() * 1000000L);
    		frame.setTimestamp(frame.getTimestamp() * 1000000L);
        
    		return frame;
    	}
    }

    /**
     * Checks the sequence numbers of specified packet and neightbor from left.
     * If sequence number decrease exchanges place of packets.
     *
     * @param p the index of packet to check.
     */
    private void sort(int p) {
        int q = dec(p);

        if (q == readCursor) {
            return;
        }

        if (queue[q] == null) {
            return;
        }

        if (queue[p].getSequenceNumber() < queue[q].getSequenceNumber()) {
            Frame temp = queue[p];
            queue[p] = queue[q];
            queue[q] = temp;
            sort(q);
        }
    }

    /**
     * Increments read cursor position
     */
    private void updateReadPosition() {
        readCursor++;
        if (readCursor == queue.length) {
            readCursor = 0;
        }
        len--;
    }

    /**
     * Increments write cursor position
     */
    private void updateWritePosition() {
        writeCursor++;
        if (writeCursor == queue.length) {
            writeCursor = 0;
        }
        len++;
    }

    /**
     * Decrements specified cursor
     *
     * @param i the cursor position.
     * @return new value for the cursor.
     */
    private int dec(int i) {
        i--;
        return i == -1 ? queue.length - 1 : i;
    }

    /**
     * Resets buffer.
     */
    public void reset() {
        readCursor = 0;
        writeCursor = -1;
        len = -1;
        isn = -1;

        for (int i = 0; i < queue.length; i++) {
            queue[i] = null;
        }
    }

}
