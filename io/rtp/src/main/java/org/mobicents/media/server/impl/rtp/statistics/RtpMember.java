package org.mobicents.media.server.impl.rtp.statistics;

import java.util.Date;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpReportBlock;
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
public class RtpMember {

	private static final Logger logger = Logger.getLogger(RtpMember.class);
	
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

	public RtpMember(RtpClock clock, long ssrc, String cname) {
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
	 * Gets the delay between receiving the last RTCP Sender Report (SR) packet
	 * from this source and sending this reception report block.
	 * 
	 * @return The delay between SR reports, expressed in units of 1/65536
	 *         seconds.<br>
	 *         If no SR packet has been received yet, the DLSR field is set to
	 *         zero. seconds
	 */
	public long getLastSRdelay() {
		if (this.lastSrReceivedOn == 0) {
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
    
    private void estimateRtt(long receiptDate, long lastSR, long delaySinceSR) {
    	TimeStamp receiptNtp = new TimeStamp(new Date(receiptDate));
    	long receiptNtpTime = calculateLastSrTimestamp(receiptNtp.getSeconds(), receiptNtp.getFraction());
    	long delay = receiptNtpTime - lastSR - delaySinceSR;
    	this.roundTripDelay = (delay > 4294967L) ? 65536 : (int) ((delay * 1000L) >> 16);
		logger.info("rtt=" + receiptNtpTime + " - " + lastSR + " - " + delaySinceSR + " = " + delay + " => " + this.roundTripDelay + "ms");
    }
    
    public void onReceiveReportBlock(RtcpReportBlock reportBlock) {
		if (reportBlock.getSsrc() == this.ssrc) {
			estimateRtt(this.wallClock.getCurrentTime(), reportBlock.getLsr(), reportBlock.getDlsr());
		}
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
		} else {
			initJitter(packet);
		}
		this.lastPacketReceivedOn = rtpClock.getLocalRtpTime();
	}
	
	public void onReceiveSR(RtcpSenderReport report) {
		this.lastSrReceivedOn = this.wallClock.getCurrentTime();
		logger.info("LAST SR RECEIVED ON: "+ this.lastSrReceivedOn+", ssrc="+this.ssrc);
		this.lastSrTimestamp = calculateLastSrTimestamp(report.getNtpSec(), report.getNtpFrac());
		this.lastSrSequenceNumber = this.lastSequenceNumber;
		this.receivedSinceSR = 0;
	}
	
	/**
	 * Calculates the time stamp of the last received SR.
	 * 
	 * @param ntp
	 *            The most significant word of the NTP time stamp
	 * @return The middle 32 bits out of 64 in the NTP timestamp received as
	 *         part of the most recent RTCP sender report (SR).
	 */
	static long calculateLastSrTimestamp(long ntp1, long ntp2) {
		byte[] high = uIntLongToByteWord(ntp1);
		byte[] low = uIntLongToByteWord(ntp1);
		low[3] = low[1];
		low[2] = low[0];
		low[1] = high[3];
		low[0] = high[2];
		return bytesToUIntLong(low, 0);
	}
	
	/** 
	 * Converts an unsigned 32 bit integer, stored in a long, into an array of bytes.
	 * 
	 * @param j a long
	 * @return byte[4] representing the unsigned integer, most significant bit first. 
	 */
	static byte[] uIntLongToByteWord(long j) {
		int i = (int) j;
		byte[] byteWord = new byte[4];
		byteWord[0] = (byte) ((i >>> 24) & 0x000000FF);
		byteWord[1] = (byte) ((i >> 16) & 0x000000FF);
		byteWord[2] = (byte) ((i >> 8) & 0x000000FF);
		byteWord[3] = (byte) (i & 0x00FF);
		return byteWord;
	}
	
	/** 
	 * Combines four bytes (most significant bit first) into a 32 bit unsigned integer.
	 * 
	 * @param bytes
	 * @param index of most significant byte
	 * @return long with the 32 bit unsigned integer
	 */
	static long bytesToUIntLong(byte[] bytes, int index) {
		long accum = 0;
		int i = 3;
		for (int shiftBy = 0; shiftBy < 32; shiftBy += 8 ) {
			accum |= ( (long)( bytes[index + i] & 0xff ) ) << shiftBy;
			i--;
		}
		return accum;
	}

}
