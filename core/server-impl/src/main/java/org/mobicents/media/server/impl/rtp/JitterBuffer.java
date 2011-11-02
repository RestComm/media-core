/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.rtp;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.mobicents.media.Format;
import org.mobicents.media.Server;

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
 * @author Oleg Kulikov
 * @author amit bhayani
 * @author baranowb
 */
public class JitterBuffer implements Serializable {

    static final int QUEUE_SIZE = 100;
    static final int RTP_SEQ_MAX = 65535;
    /**
     * Value which indicates if diff indicates
     */
    static final int LATE_THRESHOLD = QUEUE_SIZE - RTP_SEQ_MAX;
    private int jitter;
    private boolean readStarted = true;
    private boolean writeStarted = false;
    private RtpPacket[] queue = new RtpPacket[QUEUE_SIZE];
    private int readCursor;
    private int writeCursor;
    private volatile boolean ready = false;
    private long duration;
    private long timestamp;
    private Format format;
    private RtpClock clock;
    private static Logger logger = Logger.getLogger(JitterBuffer.class);
    private long drift;
    private long r,  s;
    private double j,  jm;
    
    /**
     * Reference to Sequence Number of first RTP Packet received
     */
    private int firstSeqNumberReceived = 0;
    
    /**
     * The time stamp when last RTP Packet was received from far end. Used for creation of RTCP RR
     */
    private long lastRtpPackReceivedTimeStamp;
    
    private int seqNoCycles = 0;

    /**
     * Creates new instance of jitter.
     * 
     * @param fmt
     *            the format of the received media
     * @param jitter
     *            the size of the jitter in milliseconds.
     */
    public JitterBuffer(int jitter) {
        this.jitter = jitter;
    }

    public void setClock(RtpClock clock) {
        this.clock = clock;
        if (format != null) {
            clock.setFormat(format);
        }
    }

    public void setFormat(Format format) {
        this.format = format;
        if (clock != null && format != Format.ANY) {
            clock.setFormat(format);
        }
    }

    public int getJitter() {
        return jitter;
    }

    public double getInterArrivalJitter() {
        return j;
    }

    public double getMaxJitter() {
        return jm;
    }

    public void write(RtpPacket packet) {
        //calculate time using absolute clock
        if (logger.isTraceEnabled()) {
            logger.trace("Receive " + packet);
        }
        lastRtpPackReceivedTimeStamp = Server.scheduler.getTimestamp();
        //calculate time using rtp clock
        long t = clock.getTime(packet.getTimestamp());
        packet.setTime(t);

        //calculating inter-arrival jitter
        if (r > 0 && s > 0) {
            long D = (lastRtpPackReceivedTimeStamp - r) - (packet.getTime() - s);
            if (D < 0) {
                D = -D;
            }
            j = j + (D - j) / 16;
            if (jm < j) {
                jm = j;
            }
        }

        s = packet.getTime();
        r = lastRtpPackReceivedTimeStamp;

//        if (ready && readStarted && t < timestamp) {
            //read process already started, buffer is full and packet is outstanding
            //packet should be discarded
//            logger.warn("Packet " + packet + " is discarded by jitter buffer( packet time=" + t + ", current time " + (timestamp));
//            return;
//        }

        if (!writeStarted) {
            //this is a first arrived packet
            queue[0] = packet;
            writeStarted = true;
            firstSeqNumberReceived = packet.getSeqNumber();
        } else {
            //the last received packet
            RtpPacket prev = queue[writeCursor];
            long diff = packet.getSeqNumber() - prev.getSeqNumber();
            this.process(diff, packet, prev);

        }

        if (!ready && duration > (jitter)) {
            ready = true;
        }
    }
    
    private void process(long diff, RtpPacket packet, RtpPacket prev){
        //normaly just received packet must be next in sequence
        if (diff == 1) {
            //everything is fine. writing packet and calculating duration 
            //for previous one
        	long lduration = packet.getTime() - prev.getTime();
            if(detectSilenceGap(lduration, packet,prev))
            {
            	return;
            }
        	this.writeCursor = inc(this.writeCursor, 1);
            checkSimpleOverflow(this.writeCursor);
            this.queue[this.writeCursor] = packet;
            prev.setDuration(lduration);
            this.duration += prev.getDuration();
        } else if (diff > 1) {
            //this packet arrives before another one
            //we do not know the fate of the missed packet(s) 
            //so we are updating duration like all missed packets are lost         
        	long lduration = packet.getTime() - prev.getTime();
        	//potentially dangerous, but if gap is big enough, we can assume silence.
        	//if no, only one buffer will be missing.
            if(detectSilenceGap(lduration, packet,prev))
            {
            	return;
            }
            prev.setDuration(lduration);

            //but we will leave empty slots for missed packet(s) and give
            //them chance to arrive in time.
            int nextWriteCursor = inc(writeCursor, (int) diff);
            checkPositiveOverflow(nextWriteCursor, diff);
            this.writeCursor = nextWriteCursor;
            this.queue[this.writeCursor] = packet;

            this.duration += prev.getDuration();
        } else if (diff <= LATE_THRESHOLD) { 
        	//diff <= LATE_THRESHOLD - this means that diff was so small(negative) that it indicates cseq switch, in 
        	//case of diff> LATE_THRESHOLD - its just missing packet
        	//This is Sequence number roll over, and falls into "else" below.
        	this.seqNoCycles ++;
        	diff = diff + RTP_SEQ_MAX; //Make the difference a +ve number
        	process(diff, packet, prev);
        } else {
            //diff < 0? this is missing packets and it arrives not to late 
            //so we can process it or its cseq overlap, we need to check for that. 

            int rightIndex = this.writeCursor;

            //inserting this packet in its slot
            this.writeCursor = inc(this.writeCursor, (int) diff);
            this.queue[this.writeCursor] = packet;

            //now we need to update duration of the packet in front of this one and
            //duration of this packet itself too

            //searching left neightbor packet
            int i = dec(this.writeCursor, 1);
            int count = 0;
            while (this.queue[i] == null && count < this.queue.length - 1) {
                i = dec(i, 1);
                count++;
            }

            this.queue[i].setDuration(packet.getTime() - this.queue[i].getTime());

            //now searching right neightbor packet
            i = inc(this.writeCursor, 1);
            while (this.queue[i] == null && i < rightIndex) {
                i = inc(i, 1);
            }

            packet.setDuration(this.queue[i].getTime() - packet.getTime());

        //the duration of the buffer is not changed!
        }    	
    }

    /**
	 * @return
	 */
	private boolean isSinglePacketPresent() {
		return this.readCursor == this.writeCursor;
	}

	private boolean detectSilenceGap(long lduration, RtpPacket packet,RtpPacket prev)
	{
		//assumption: 1 packet in buffer
		//check: if time gap is bigger than avg for all previous packets, it was silence
		//action: flush buffer and start clean write from zero
		if(isSinglePacketPresent())
        {
			//use when there is packet, silence,rest of packets....
			int seq = prev.getSeqNumber()-firstSeqNumberReceived;
			if(seq == 0)
			{
				seq = 1;
			} else if(seq < 0)
			{
				seq+=RTP_SEQ_MAX;
			}
			//will getTime(). return consistent result?
			long avgPacketDuration = prev.getTime()/(seq + seqNoCycles*RTP_SEQ_MAX);
        	//if 3xavg<lduration, it may be silence... since we have single packet
        	if(avgPacketDuration*3<lduration ) //3 should be good number? 
        	{
        		//reset buffer, 
        		this.softReset();//baranowb: is this ok? we are not reseting firstSeq and seqCycles.
        						 //cause getTime will always ++, we need to keep that for life time of buffer.
        		this.write(packet);
        		return true;
        	}	
        }
      
        return false;
	}
	
	private void checkSimpleOverflow(int toBeWrittenCurson) {
        //here we check if we are going to overwrite packet, if so, we must decrement duration
        if (this.readCursor == toBeWrittenCurson) {
            //its a flip. not a best situation ....
            RtpPacket removed = this.queue[this.readCursor];
            this.queue[this.readCursor] = null;
            this.readCursor = inc(this.readCursor, 1);
            this.duration -= removed.getDuration();
        }

    }

    private void checkPositiveOverflow(int nextWriteCursor, long diff) {
        //here we get if nextWriteCursor-writeCursor > 1
        //that means we have more than one packet to discard.
        //duration of prev packet is set correctly, now lets remove all other packets.

        //nasty check, lets see if we should act
        //r - readCursor,w - writeCursor, nw - new(next)WriteCursor

        long boundry = this.writeCursor + diff;
        if (boundry >= QUEUE_SIZE) {
            //all possible flip cases
            if ((this.readCursor > this.writeCursor) && (nextWriteCursor < this.readCursor)) {
                //case II nw_w_r
                this.cleanBufferOnPositiveOverflow(nextWriteCursor);
            } else if ((this.readCursor < this.writeCursor) && (nextWriteCursor >= this.readCursor)) {
                //case III r_nw_w
                this.cleanBufferOnPositiveOverflow(nextWriteCursor);
            } else {
            }
        } else {
            if ((this.readCursor > this.writeCursor) && (nextWriteCursor >= this.readCursor)) {
                //case I w_r_nw
                this.cleanBufferOnPositiveOverflow(nextWriteCursor);
            } else {
            }
        }
    }

    private void cleanBufferOnPositiveOverflow(int nextWriteCursor) {
        //start from right side, there is a good chance we can stumble on null section
        //that is no data, so we dont have to do anything and can terminate clean, saves cycles.
        int oldRead = dec(this.readCursor,1);
        this.readCursor = inc(nextWriteCursor, 1);
        while (nextWriteCursor != oldRead) {
            //we clean
            if (this.queue[nextWriteCursor] == null) {
                return;
            }
            RtpPacket removed = this.queue[nextWriteCursor];
            this.queue[nextWriteCursor] = null;
            this.duration -= removed.getDuration();
            nextWriteCursor = dec(nextWriteCursor, 1);
        }

    //this.readCursor = 
    }

    private int inc(int a, int diff) {
        int res = a + diff;
        if (res >= queue.length) {
            res = res - queue.length;
        }
        return res;
    }

    private int dec(int a, int diff) {
        //FIXME: baranowb: profile and check if module is faster, should be!!!
        int res = a - diff;
        if (res < 0) {
            //res is negative
            res = queue.length + res;
        }
        return res;
    }
    /**
     * Resets read/write aspects of buffer. For instance after period of silence, buffer calls this method to 
     * prepare itself for continuation of stream.
     */
    public void softReset() {
    	
        duration = 0;
        
        drift = 0;
        r = 0;
        s = 0;

        ready = false;
        readStarted = true;
        writeStarted = false;

        readCursor = 0;
        writeCursor = 0;
        lastRtpPackReceivedTimeStamp = 0;
    }
    
    public void reset()
    {
    	 softReset();
    	 clock.reset();
    	 seqNoCycles = 0;
         firstSeqNumberReceived = 0;
    }

    /**
     * 
     * @return
     */
    public RtpPacket read(long timestamp) {
        //discard buffer is buffer is not full yet
        if (!ready) {
            return null;
        }

        //before read any packets let's compute time drift between
        //remote and local peers.
        if (!readStarted) {
            readStarted = true;
            drift = queue[0].getTime() - timestamp;
        }

        //now our clock shows time specified as timestamp parameter
        //the same time measured using remote clock is as follows:
        this.timestamp = timestamp + drift;

        //we have to read from buffer all packets with timestamp less then current 
        //time measured using remote clock.

        //before reading we have to be sure that buffer is not empty
        //we can do it if compare absolute read and write indexes
        //when packet queue is not empty absolute read index is less then write index
        if (duration == 0) {
            return null;
        }

        //fill media buffer
        RtpPacket packet = queue[readCursor];
        queue[readCursor] = null;

        duration -= packet.getDuration();
        readCursor = inc(readCursor, 1);
        // lets shift, to point to next valid data
        while (duration >= 0 && queue[readCursor] == null) {
            this.readCursor = inc(this.readCursor, 1);
        }
        return packet;
    }

	public int getSeqNoCycles() {
		return seqNoCycles;
	}
    
    public RtpPacket getLastRtpPacketRecd(){
    	return queue[writeCursor];
    }

	public long getLastRtpPackReceivedTimeStamp() {
		return lastRtpPackReceivedTimeStamp;
	}
	
	/**
	 * Gives the expected Packet Count at this moment
	 * @return
	 */
	public int getExpectedPacketCount(){
		RtpPacket rtpPacket = getLastRtpPacketRecd();	
		if(rtpPacket!=null){
			return rtpPacket.getSeqNumber() + this.seqNoCycles * 65535 - firstSeqNumberReceived + 1;
		}
		return 0;
	}

}
