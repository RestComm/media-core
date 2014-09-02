package org.mobicents.media.server.impl.rtp.statistics;

import java.util.Arrays;

import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
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
	private long receivedPackets;
	private long receivedOctets;
	private long receivedSinceSR;
	private long lastPacketReceivedOn;
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
	private long lastSrTimestamp;
	private long lastSrReceivedOn;
	private long lastSrSequenceNumber;

	public Member(RtpClock clock, long ssrc, String cname) {
		// Core elements
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();

		// Member data
		this.ssrc = ssrc;
		this.cname = cname;

		// Packet stats
		this.receivedPackets = 0;
		this.receivedOctets = 0;
		this.receivedSinceSR = 0;
		this.lastPacketReceivedOn = -1;

		this.firstSequenceNumber = -1;
		this.lastSequenceNumber = 0;
		this.sequenceCycle = 0;

		// Jitter
		this.currentTransit = 0;
		this.jitter = -1;

		// RTCP
		this.lastSrTimestamp = 0;
		this.lastSrReceivedOn = -1;
		this.lastSrSequenceNumber = -1;
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
	public long getReceivedPackets() {
		return receivedPackets;
	}

	/**
	 * Gets the total of incoming RTP octets
	 * 
	 * @return The total of received octets
	 */
	public long getReceivedOctets() {
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
		long expected = this.lastSequenceNumber - this.lastSrSequenceNumber;
		if (expected < 0) {
			expected = 65536 + expected;
		}

		long fraction = 256 * (expected - this.receivedSinceSR);
		fraction = expected > 0 ? (fraction / expected) : 0;

		return fraction;
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
	 * 
	 * @return The number of lost packets.<br>
	 *         Returns zero if loss is negative, i.e. duplicates have been
	 *         received.
	 */
	public long getPacketsLost() {
		long expected = getExtHighSequence() - this.firstSequenceNumber;
		long lost = expected - this.receivedPackets;
		return lost < 0 ? 0 : lost;
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
	 * Sets the last time an RTCP Sender Report was received from this source.
	 * 
	 * @param lastSR
	 *            The middle 32 bits out of 64 in the NTP timestamp received as
	 *            part of the most recent RTCP sender report (SR) packet.
	 */
	public void setLastSR(long lastSR) {
		this.lastSrTimestamp = lastSR;
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
		if (this.lastSrReceivedOn < 0) {
			return 0;
		}

		long delay = this.wallClock.getCurrentTime() - this.lastSrReceivedOn;
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
	protected long getExtHighSequence() {
		return (65536 * this.sequenceCycle + this.lastSequenceNumber);
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

	public void onReceiveRtp(RtpPacket packet) {
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

		if(this.lastPacketReceivedOn > 0) {
			estimateJitter(packet);
		}
		this.lastPacketReceivedOn = rtpClock.getLocalRtpTime();
	}
	
	public void onReceiveSR(RtcpSenderReport report) {
		this.lastSrReceivedOn = this.wallClock.getCurrentTime();
		this.lastSrTimestamp = calculateLastSrTimestamp(report.getNtpSec());
		this.lastSrSequenceNumber = this.lastSequenceNumber;
		this.receivedSinceSR = 0;
	}
	
	/**
	 * Calculates the time stamp of the last received SR.
	 * 
	 * @param msw
	 *            The most significant word of the NTP time stamp
	 * @param lsw
	 *            The least significant word of the NTP time stamp
	 * @return The middle 32 bits out of 64 in the NTP timestamp received as
	 *         part of the most recent RTCP sender report (SR).
	 */
	private long calculateLastSrTimestamp(long ntp) {
		byte[] ntpWord = toByteArray(ntp);
		byte[] middleWord = Arrays.copyOfRange(ntpWord, 2, 6);
		return fromBytes(middleWord);
	}
	
	/**
	 * Returns a big-endian representation of {@code value} in an 8-element byte
	 * array; equivalent to
	 * {@code ByteBuffer.allocate(8).putLong(value).array()}.
	 * <p>
	 * For example, the input value {@code 0x1213141516171819L} would yield the
	 * byte array {@code 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19}.
	 * </p>
	 * 
	 * @param value
	 *            The 64-bit number to be converted
	 * @return The byte array representing the number
	 */
	private byte[] toByteArray(long value) {
	    byte[] result = new byte[8];
	    for (int i = 7; i >= 0; i--) {
	      result[i] = (byte) (value & 0xffL);
	      value >>= 8;
	    }
	    return result;
	}
	
	/**
	 * Returns the {@code long} value whose byte representation is the given 8
	 * bytes, in big-endian order
	 * 
	 * @param b
	 *            The byte array to be converted
	 * @return The 32-bit number that represents the byte array
	 */
	private long fromBytes(byte[] b) {
		return (b[0] & 0xFFL) << 24 
				| (b[1] & 0xFFL) << 16
				| (b[2] & 0xFFL) << 8 
				| (b[3] & 0xFFL);
	}

}
