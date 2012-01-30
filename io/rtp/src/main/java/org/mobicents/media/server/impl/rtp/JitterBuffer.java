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
import java.util.concurrent.Semaphore;

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
    private ArrayList<Frame> queue = new ArrayList(QUEUE_SIZE);
    
    //semaphore to correctly organize frames in queue
    private Semaphore writeSemaphore=new Semaphore(1);
    
    //RTP clock
    private RtpClock rtpClock;
    //first received sequence number
    private long isn = -1;

    //allowed jitter
    private long jitter;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long arrivalDeadLine = 0;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long droppedInRaw = 0;
    
    private long r, s;
    private double j, jm;

    //The number of dropped packets
    private int dropCount;

    private int readCount=0,acceptedCount=0;
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
    
    /**
     * Creates new instance of jitter.
     * 
     * @param clock the rtp clock.
     */
    public JitterBuffer(RtpClock clock, int jitter) {
        this.rtpClock = clock;
        this.jitter = rtpClock.convertToRtpTime(jitter);
        this.dtmfConverter = new DtmfConverter(this);
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
    	//checking format
    	if (this.format == null) {
    		//if format is not known yet assign the format of this packet
    		this.format = rtpFormats.find(packet.getPayloadType());
    		
    		if(this.format!=null)
    			System.out.println("Format has been changed: " + this.format.toString());
    	} else if (this.format.getID() != packet.getPayloadType()) {
    		//format has been changed 
    		this.format = rtpFormats.find(packet.getPayloadType());
    		
    		if(this.format!=null)
    			System.out.println("Format has been changed: " + this.format.toString());
    	}

    	//ignore unknow packet
    	if (this.format == null) {
    		//unknown packet
    		return;
    	}
        
    	//if this is first packet then synchronize clock
    	if (isn == -1) {
    		rtpClock.synchronize(packet.getTimestamp());
    		isn = packet.getSeqNumber();
    	}
    	
    	//update clock rate
    	rtpClock.setClockRate(this.format.getClockRate());            		    		
        
    	Frame f=null;
    	if (this.format != null && this.format.getFormat().matches(dtmf)) {
    		dtmfConverter.push(packet);
    		return;
    	} else {
    		//drop outstanding packets
    		//packet is outstanding if its timestamp of arrived packet is less
    		//then consumer media time
    		if (packet.getTimestamp() < this.arrivalDeadLine) {
    			System.out.println("drop packet: dead line=" + arrivalDeadLine
                    + ", packet time=" + packet.getTimestamp() + ", seq=" + packet.getSeqNumber()
                    + ", payload length=" + packet.getPayloadLength());
    			dropCount++;
    			
    			//checking if not dropping too much  			
    			droppedInRaw++;
    			if(droppedInRaw==QUEUE_SIZE/2)
    				arrivalDeadLine=0;
    			else
    				return;
    		}
    			
    		f=Memory.allocate(packet.getPayloadLength());
    		//put packet into buffer irrespective of its sequence number
    		f.setHeader(null);
    		f.setSequenceNumber(packet.getSeqNumber());
    		//here time is in milliseconds
    		f.setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
    		f.setOffset(0);
    		f.setLength(packet.getPayloadLength());
    		packet.getPyalod(f.getData(), 0);

    		//set format
    		f.setFormat(this.format.getFormat());
    	}
    		
    	//make checks only if have packet
    	if(f!=null)
    	{    
    		droppedInRaw=0;
    		try
    		{
    			//obtaining semaphore aquire and writing frame to queue
    			writeSemaphore.acquire();
    		}
    		catch(InterruptedException e)
    		{}
    		
    		//find correct position to insert a packet    			
    		int currIndex=queue.size()-1;
    		while (currIndex>=0 && queue.get(currIndex).getSequenceNumber() > f.getSequenceNumber())
    			currIndex--;
    			    		
    		if(currIndex>=0 && queue.get(currIndex).getSequenceNumber() == f.getSequenceNumber())
    		{
    			//duplicate packet
    			writeSemaphore.release();
    			return;
    		}
    				    			
    		queue.add(currIndex+1, f);
    			
    		//recalculate duration of each frame in queue and overall duration , since we could insert the
    		//frame in the middle of the queue    			
    		duration=0;    			
    		if(queue.size()>1)
    			duration=queue.get(queue.size()-1).getTimestamp() - queue.get(0).getTimestamp();
    		
    		for(int i=0;i<queue.size()-1;i++)
    		{
    			//duration measured by wall clock
    			long d = queue.get(i+1).getTimestamp() - queue.get(i).getTimestamp();
    			//in case of RFC2833 event timestamp remains same
    			if (d > 0)    				
    				queue.get(i).setDuration(d);    					
    			else
    				queue.get(i).setDuration(0);
    		}
    			
    		//if overall duration is negative we have some mess here,try to reset
    		if(duration<0 && queue.size()>1)
    		{
    			writeSemaphore.release();
    			reset();
    			return;
    		}
    			    			
    		//overflow?
    		//only now remove packet if overflow , possibly the same packet we just received
    		if (queue.size()>QUEUE_SIZE) {
    			//System.out.println("Buffer overflow");    			
    			dropCount++;        			
    			queue.remove(0);    				
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
    			ready = duration >= jitter && queue.size() > 1;
    			if (ready) {    				
    				if (listener != null) {
    					listener.onFill();
    				}
    			}
    		}
    		
    		//releasing semaphore
    		writeSemaphore.release();
    	}
    }
    
    public void pushFrame(Frame f)
    {
    	droppedInRaw=0;
		try
		{
			//obtaining semaphore aquire and writing frame to queue
			writeSemaphore.acquire();
		}
		catch(InterruptedException e)
		{}
		
		//find correct position to insert a packet    			
		int currIndex=queue.size()-1;
		while (currIndex>=0 && queue.get(currIndex).getSequenceNumber() > f.getSequenceNumber())
			currIndex--;
			    		
		if(currIndex>=0 && queue.get(currIndex).getSequenceNumber() == f.getSequenceNumber())
		{
			//duplicate packet
			writeSemaphore.release();
			return;
		}
				    			
		queue.add(currIndex+1, f);
			
		//recalculate duration of each frame in queue and overall duration , since we could insert the
		//frame in the middle of the queue    			
		duration=0;    			
		if(queue.size()>1)
			duration=queue.get(queue.size()-1).getTimestamp() - queue.get(0).getTimestamp();				
			
		//overflow?
		//only now remove packet if overflow , possibly the same packet we just received
		if (queue.size()>QUEUE_SIZE) {
			//System.out.println("Buffer overflow");    			
			dropCount++;        			
			queue.remove(0);    				
		}    		
			
		//check if this buffer already full
		if (!ready) {    			
			ready = duration >= jitter && queue.size() > 1;
			if (ready) {    				
				if (listener != null) {
					listener.onFill();
				}
			}
		}
		
		//releasing semaphore
		writeSemaphore.release();
    }

    /**
     * Polls packet from buffer's head.
     *
     * @param timestamp the media time measured by reader
     * @return the media frame.
     */
    public Frame read(long timestamp) {
    	if (queue.size()==0) {
    		return null;
    	}
    		
    	//extract packet
    	Frame frame = queue.remove(0);
    		
    	//buffer empty now? - change ready flag.
    	if (queue.size() == 0) {
    		this.ready = false;
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
    	queue.clear();
    }
    
    public void restart() {
    	this.ready=false;
    	arrivalDeadLine = 0;
    	dropCount=0;
    	droppedInRaw=0;
    	format=null;
    	isn=-1;
    }
}
