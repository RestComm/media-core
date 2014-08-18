package org.mobicents.media.server.impl.rtp.statistics;

import java.util.concurrent.TimeUnit;

import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.scheduler.Clock;

/**
 * Holds statistics for a member of an RTP session.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class Member {

	// Core elements
	private final RtpClock rtpClock;
	private final Clock wallClock;

	// Member data
	private long ssrc;
	private String cname;

	// Packet stats
	private int lostPackets;
	private long receivedPackets;
	private long receivedOctets;
	private long receivedSinceSR;
	private long lastPacketReceivedOn;
	private long lastPacketTimestamp;
	private long firstSequenceNumber;
	private long lastSequenceNumber;
	private int sequenceCycle;
	
	// Jitter
	/**
	 * Measures the relative time it takes for an RTP packet to arrive from the
	 * remote server to MMS.<br>
	 * Used to calculate network jitter.
	 */
	private long currentTransit;
	private long jitter;
	
	// RTCP
	private long lastSR;
	private long lastSRSequenceNumber;

	public Member(RtpClock clock, long ssrc, String cname) {
		// Core elements
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();
		
		// Member data
		this.ssrc = ssrc;
		this.cname = cname;
		
		// Packet stats
		this.lostPackets = 0;
		this.receivedPackets = 0;
		this.receivedOctets = 0;
		this.receivedSinceSR = 0;
		this.lastPacketReceivedOn = -1;
		this.lastPacketTimestamp = 0;
		
		this.firstSequenceNumber = -1;
		this.lastSequenceNumber = 0;
		this.sequenceCycle = 0;

		// Jitter
		this.currentTransit = 0;
		this.jitter = -1;
		
		// RTCP
		this.lastSR = 0;
		this.lastSRSequenceNumber = -1;
	}
	
	public Member(RtpClock clock, long ssrc) {
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
	
	public String getCname() {
		return cname;
	}
	
	public void setCname(String cname) {
		this.cname = cname;
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
		long expected = this.lastSequenceNumber - this.lastSRSequenceNumber;
		if (expected < 0) {
			expected = 65536 + expected;
		}

		long fraction = 256 * (expected - this.receivedSinceSR);
		fraction = expected > 0 ? (fraction / expected) : 0;

		// Clear counters
		this.receivedSinceSR = 0;
		this.lastSRSequenceNumber = this.lastSequenceNumber;

		return fraction;
	}
	
	/**
	 * Gets the total number of RTP data packets from this source that have been
	 * lost since the beginning of reception.
	 * 
	 * @return The number of lost packets.<br>
	 *         Returns zero if loss is negative, i.e. duplicates have been
	 *         received.
	 */
	public long getLostPackets() {
		long lost = getExtHighSequence() - this.firstSequenceNumber;
		return lost < 0 ? 0 : lost;
	}

	/**
	 * Sets the total number of RTP data packets from this source that have been
	 * lost since the beginning of reception.
	 * 
	 * This number is defined to be the number of packets expected less the
	 * number of packets actually received, where the number of packets received
	 * includes any which are late or duplicates. Thus, packets that arrive late
	 * are not counted as lost, and the loss may be negative if there are
	 * duplicates. The number of packets expected is defined to be the extended
	 * last sequence number received, as defined next, less the initial sequence
	 * number received.
	 * 
	 * @param lostPackets
	 *            the number of packets lost
	 */
	public void setLostPackets(int lostPackets) {
		this.lostPackets = lostPackets;
	}

	/**
	 * Gets the highest sequence number received in an RTP data packet from this
	 * source.
	 * 
	 * @return The highest sequence number on this source
	 */
	public long getSequenceNumber() {
		return lastSequenceNumber;
	}

	/**
	 * Sets the highest sequence number received in an RTP data packet from this
	 * source.
	 * 
	 * @param sequenceNumber
	 *            The highest sequence number on this source
	 */
	public void setSequenceNumber(long sequenceNumber) {
		this.lastSequenceNumber = sequenceNumber;
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
	 * Sets the count of sequence number cycles
	 * 
	 * @param sequenceCycle
	 */
	public void setSequenceCycle(int sequenceCycle) {
		this.sequenceCycle = sequenceCycle;
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
	 * Sets the estimate jitter for this source.
	 * 
	 * @param jitter
	 *            The statistical variance of the RTP data packet interarrival
	 *            time, measured in timestamp units and expressed as an unsigned
	 *            integer.
	 */
	public void setJitter(int jitter) {
		this.jitter = jitter;
	}

	/**
	 * Gets the last time an RTCP Sender Report was received from this source.
	 * 
	 * @return The middle 32 bits out of 64 in the NTP timestamp received as
	 *         part of the most recent RTCP sender report (SR) packet.<br>
	 *         If no SR has been received yet, returns zero.
	 */
	public long getLastSR() {
		return lastSR;
	}

	/**
	 * Sets the last time an RTCP Sender Report was received from this source.
	 * 
	 * @param lastSR
	 *            The middle 32 bits out of 64 in the NTP timestamp received as
	 *            part of the most recent RTCP sender report (SR) packet.
	 */
	public void setLastSR(long lastSR) {
		this.lastSR = lastSR;
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
		if(this.receivedSinceSR < 0) {
			return 0;
		}
		
		long delay = this.wallClock.getTime() - this.receivedSinceSR;
		// convert nanoseconds to units 1/65536 seconds
		return delay * TimeUnit.SECONDS.toNanos(65536);
	}

	/**
	 * Calculates the extended highest sequence received by adding the last
	 * sequence number to 65536 times the number of times the sequence counter
	 * has rolled over.
	 * 
	 * @return extended highest sequence
	 */
	protected long getExtHighSequence() {
		return (65536 * this.sequenceCycle + this.lastSequenceNumber);
	}

	/**
	 * Calculates interarrival jitter interval
	 * @param packet
	 * @return
	 */
	private long estimateJitter(RtpPacket packet) {
		long arrival = rtpClock.getLocalRtpTime();
		long newPacketTimestamp = packet.getTimestamp();
		long transit = arrival - newPacketTimestamp;
		long d = transit - this.currentTransit;

		if (d < 0) {
			d = -d;
		}

		this.currentTransit = transit;
		return this.jitter + d - ((this.jitter + 8) >> 4);
	}

	public void onReceive(RtpPacket packet) {
		int seqNumber = packet.getSeqNumber();

		if (this.firstSequenceNumber < 0) {
			this.firstSequenceNumber = seqNumber;
		}

		this.receivedSinceSR++;
		this.receivedPackets++;
		this.receivedOctets += packet.getLength();

		if (this.lastSequenceNumber < seqNumber) {
			// In-line packet, all is good
			this.lastSequenceNumber = seqNumber;
		} else if (this.lastSequenceNumber - seqNumber > 100) {
			// Sequence counter rolled over
			this.lastSequenceNumber = seqNumber;
			this.sequenceCycle++;
		} else {
			// Probably a duplicate or late arrival
		}

		this.jitter = estimateJitter(packet);

		this.lastPacketReceivedOn = rtpClock.getLocalRtpTime();
		this.lastPacketTimestamp = packet.getTimestamp();
	}

}
