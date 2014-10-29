package org.mobicents.media.server.impl.rtp.statistics;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtcp.ntp.NtpUtils;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.scheduler.Clock;

/**
 * Holds statistics for a member of an RTP session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpMember {

	private static final Logger logger = Logger.getLogger(RtpMember.class);

	private static final int RTP_SEQ_MOD = 65536;
	private static final int MAX_DROPOUT = 3000;
	private static final int MAX_MISORDER = 100;
	private static final int MIN_SEQUENTIAL = 2;
	
	// Core elements
	private final RtpClock rtpClock;
	private final Clock wallClock;

	// Member data
	private long ssrc;
	private String cname;

	// Packet stats
	private long receivedPackets;
	private long receivedOctets;
	private long receivedSinceSR;
	private int roundTripDelay;
	private long lastPacketReceivedOn;
	private int firstSequenceNumber;
	private int highestSequence;
	private int extHighestSequence;
	private int sequenceCycle;
	private int badSequence;
	private int probation;
	private long receivedPrior;
	private long expectedPrior;
	private int fractionLost;
	
	private long expectedPackets;

	// Jitter
	/**
	 * Measures the relative time it takes for an RTP packet to arrive from the
	 * remote server to MMS.<br>
	 * Used to calculate network jitter.
	 */
	private long currentTransit;
	private long jitter;

	// RTCP
	private long lastSrTimestamp;
	private long lastSrReceivedOn;
	private long lastSrSequenceNumber;

	public RtpMember(RtpClock clock, long ssrc, String cname) {
		// Core elements
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();

		// Member data
		this.ssrc = ssrc;
		this.cname = cname;

		// Packet stats
		this.expectedPackets = 0;
		this.receivedPackets = 0;
		this.receivedOctets = 0;
		this.receivedSinceSR = 0;
		this.lastPacketReceivedOn = -1;

		this.firstSequenceNumber = -1;
		this.highestSequence = 0;
		this.badSequence = 0;
		this.sequenceCycle = 0;
		this.probation = 0;
		this.receivedPrior = 0;
		this.expectedPrior = 0;
		this.fractionLost = 0;

		// Jitter
		this.currentTransit = 0;
		this.jitter = -1;

		// RTCP
		this.lastSrTimestamp = 0;
		this.lastSrReceivedOn = 0;
		this.lastSrSequenceNumber = -1;
		this.roundTripDelay = 0;
	}

	public RtpMember(RtpClock clock, long ssrc) {
		this(clock, ssrc, "");
	}

	/**
	 * Gets the SSRC identifier of the source to which the information in this
	 * reception report block pertains
	 * 
	 * @return The SSRC identifier
	 */
	public long getSsrc() {
		return ssrc;
	}

	/**
	 * Gets the CNAME of this member
	 * 
	 * @return The CNAME of the member
	 */
	public String getCname() {
		return cname;
	}

	/**
	 * Sets the CNAME of the member
	 * 
	 * @param cname
	 *            The CNAME of the member
	 */
	public void setCname(String cname) {
		this.cname = cname;
	}

	/**
	 * Gets the total number of incoming RTP packets
	 * 
	 * @return The number of packets received
	 */
	public long getPacketsReceived() {
		return receivedPackets;
	}

	/**
	 * Gets the total of incoming RTP octets
	 * 
	 * @return The total of received octets
	 */
	public long getOctetsReceived() {
		return receivedOctets;
	}

	/**
	 * Gets the number of incoming RTP packets since the last SR report was
	 * sent.
	 * 
	 * @return The number of incoming RTP packets
	 */
	public long getReceivedSinceSR() {
		return receivedSinceSR;
	}

	/**
	 * Gets the fraction of RTP data packets from this source that were lost
	 * since the previous SR or RR packet was sent, expressed as a fixed point
	 * number with the binary point at the left edge of the field. (That is
	 * equivalent to taking the integer part after multiplying the loss fraction
	 * by 256.)
	 * 
	 * @return The fraction of lost packets
	 */
	public long getFractionLost() {
//		long expected = this.lastSequenceNumber - this.lastSrSequenceNumber;
//		if (expected < 0) {
//			expected = RTP_SEQ_MOD + expected;
//		}
//
//		long fraction = 256 * (expected - this.receivedSinceSR);
//		fraction = expected > 0 ? (fraction / expected) : 0;
//
//		return fraction;
		
		long expectedInterval = this.expectedPackets - this.expectedPrior;
		this.expectedPrior = this.expectedPackets;
		
		long receivedInterval = this.receivedPackets - this.receivedPrior;
		this.receivedPrior = this.receivedPackets;
		
		long lostInterval = expectedInterval - receivedInterval;
		if(expectedInterval == 0 || lostInterval <= 0) {
			return 0;
		}
		return (lostInterval << 8) / expectedInterval;
	}
	
	/**
	 * Gets the total number of RTP data packets from this source that have been
	 * lost since the beginning of reception.
	 * <p>
	 * This number is defined to be the number of packets expected less the
	 * number of packets actually received, where the number of packets received
	 * includes any which are late or duplicates. Thus, packets that arrive late
	 * are not counted as lost, and the loss may be negative if there are
	 * duplicates.
	 * </p>
	 * <p>
	 * <b>The number of packets expected is defined to be the extended last
	 * sequence number received, as defined next, less the initial sequence
	 * number received.</b>
	 * </p>
	 * <p>
	 * Since this signed number is carried in 24 bits, it should be clamped at
	 * 0x7FFFFF for positive loss or 0x800000 for negative loss rather than
	 * wrapping around.
	 * </p>
	 * 
	 * @return The number of lost packets.<br>
	 *         Loss can be negative, i.e. duplicates have been received.
	 */
	public long getPacketsLost() {
		long lost = this.expectedPackets - this.receivedPackets;
		
		if(lost > 0x7fffff) {
			return 0x7fffff;
		}
		
		return lost;
	}

	/**
	 * Gets the count of sequence number cycles
	 * 
	 * @return The number of cycles
	 */
	public int getSequenceCycle() {
		return sequenceCycle;
	}

	/**
	 * Gets an estimate of the statistical variance of the RTP data packet
	 * interarrival time, measured in timestamp units and expressed as an
	 * unsigned integer.
	 * 
	 * @return the estimated jitter for this source
	 */
	public long getJitter() {
		return this.jitter >> 4;
	}

	/**
	 * Gets the last time an RTCP Sender Report was received from this source.
	 * 
	 * @return The middle 32 bits out of 64 in the NTP timestamp received as
	 *         part of the most recent RTCP sender report (SR) packet.<br>
	 *         If no SR has been received yet, returns zero.
	 */
	public long getLastSR() {
		return lastSrTimestamp;
	}

	/**
	 * Gets the delay between receiving the last RTCP Sender Report (SR) packet
	 * from this source and sending this reception report block.
	 * 
	 * @return The delay between SR reports, expressed in units of 1/65536
	 *         seconds.<br>
	 *         If no SR packet has been received yet, the DLSR field is set to
	 *         zero. seconds
	 */
	public long getLastSRdelay() {
		return getLastSRdelay(this.wallClock.getCurrentTime(), this.lastSrReceivedOn);
	}
	
	private long getLastSRdelay(long arrivalTime, long lastSrTime) {
		if (this.lastSrReceivedOn == 0) {
			return 0;
		}

		long delay = arrivalTime - lastSrTime;
		// convert to units 1/65536 seconds
		return (long) (delay * 65.536);
	}

	/**
	 * Calculates the extended highest sequence received by adding the last
	 * sequence number to 65536 times the number of times the sequence counter
	 * has rolled over.
	 * 
	 * @return extended highest sequence
	 */
	public long getExtHighSequence() {
		return this.extHighestSequence;
	}
	
	public int getRTT() {
		if(this.roundTripDelay > 0) {
			return this.roundTripDelay;
		}
		return 0;
	}

	/**
	 * Calculates interarrival jitter interval.
	 * 
	 * <p>
	 * <code>
	 * int transit = arrival - r->ts;<br>
	 * int d = transit - s->transit;<br>
	 * s->transit = transit;<br>
	 * if (d < 0) d = -d;<br>
	 * s->jitter += (1./16.) * ((double)d - s->jitter);<br>
	 * </code>
	 * </p>
	 * 
	 * @param packet
	 * @return
	 * @see <a
	 *      href="http://tools.ietf.org/html/rfc3550#appendix-A.8">RFC3550</a>
	 */
	private void estimateJitter(RtpPacket packet) {
		long transit = rtpClock.getLocalRtpTime() - packet.getTimestamp();
		long d = transit - this.currentTransit;
		this.currentTransit = transit;
		
		if(d < 0) {
			d = -d;
		}

		this.jitter += d - ((this.jitter + 8) >> 4);
	}
	
    private void initJitter(RtpPacket packet) {
        this.currentTransit = rtpClock.getLocalRtpTime() - packet.getTimestamp();
    }
    
    public void estimateRtt(long receiptDate, long lastSR, long delaySinceSR) {
    	TimeStamp receiptNtp = TimeStamp.getNtpTime(receiptDate);
    	long receiptNtpTime = NtpUtils.calculateLastSrTimestamp(receiptNtp.getSeconds(), receiptNtp.getFraction());
    	long delay = receiptNtpTime - lastSR - delaySinceSR;
    	this.roundTripDelay = (delay > 4294967L) ? RTP_SEQ_MOD : (int) ((delay * 1000L) >> 16);
		logger.info("rtt=" + receiptNtpTime + " - " + lastSR + " - " + delaySinceSR + " = " + delay + " => " + this.roundTripDelay + "ms");
    }
    
    private void setHighestSequence(int sequence) {
    	this.highestSequence = sequence;
		this.extHighestSequence = this.highestSequence + (RTP_SEQ_MOD * this.sequenceCycle);
		this.expectedPackets = this.extHighestSequence - this.firstSequenceNumber + 1;
    }
    
    
    private void initSequence(int sequence) {
    	this.firstSequenceNumber = sequence;
    	setHighestSequence(sequence);
    	this.badSequence = RTP_SEQ_MOD + 1; // so seq != bad_seq
    	this.sequenceCycle = 0;
    	this.receivedPrior = 0;
    	this.expectedPrior = 0;
    }
    
    private boolean updateSequence(int sequence) {
    	int delta = Math.max(0, sequence - this.highestSequence);

    	if(this.probation > 0) {
    		// packet is in sequence
    		if(sequence == this.highestSequence + 1) {
    			this.probation--;
    			setHighestSequence(sequence);
    			
    			if(this.probation == 0) {
    				initSequence(sequence);
    				return true;
    			}
    		} else {
    			this.probation = MIN_SEQUENTIAL - 1;
    			setHighestSequence(sequence);
    		}
    		return false;
    	} else if (delta < MAX_DROPOUT) {
    		// in order, with permissible gap
    		if(sequence < this.highestSequence) {
    			// sequence number wrapped - count another 64k cycle
    			this.sequenceCycle += RTP_SEQ_MOD;
    		}
    		setHighestSequence(sequence);
    	} else if (delta <= RTP_SEQ_MOD - MAX_MISORDER) {
    		// the sequence number made a very large jump
    		if(sequence == this.badSequence) {
				/*
				 * Two sequential packets -- assume that the other side
				 * restarted without telling us so just re-sync (i.e., pretend
				 * this was the first packet).
				 */
                initSequence(sequence);
    		} else {
    			this.badSequence = (sequence + 1) & (RTP_SEQ_MOD - 1);
                return false;
    		}
    	} else {
    		// duplicate or reordered packet
    	}
    	return true;
    }
    
	public void onReceiveRtp(RtpPacket packet) {
		this.receivedSinceSR++;
		this.receivedPackets++;
		this.receivedOctets += packet.getPayloadLength();

		int sequence = packet.getSeqNumber();
		
		/*
		 * When a new source is heard for the first time, that is, its SSRC
		 * identifier is not in the table (see Section 8.2), and the per-source
		 * state is allocated for it, s->probation is set to the number of
		 * sequential packets required before declaring a source valid
		 * (parameter MIN_SEQUENTIAL) and other variables are initialized
		 */
		if (this.firstSequenceNumber < 0) {
			initSequence(sequence);
			this.highestSequence = sequence - 1;
			this.probation = MIN_SEQUENTIAL;
		} else {
			updateSequence(sequence);
		}

		if(this.lastPacketReceivedOn > 0) {
			estimateJitter(packet);
		} else {
			initJitter(packet);
		}
		this.lastPacketReceivedOn = rtpClock.getLocalRtpTime();
	}
	
	public void onReceiveSR(RtcpSenderReport report) {
		// Update statistics
		this.lastSrTimestamp = report.getNtpTs();
		this.lastSrReceivedOn = this.wallClock.getCurrentTime();
		this.lastSrSequenceNumber = this.highestSequence;
		this.receivedSinceSR = 0;
	}
	
}
