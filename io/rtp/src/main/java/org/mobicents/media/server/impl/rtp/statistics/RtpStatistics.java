package org.mobicents.media.server.impl.rtp.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtcp.RtcpPacketType;
import org.mobicents.media.server.impl.rtcp.RtcpReport;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.scheduler.Clock;

/**
 * Encapsulates statistics of an RTP/RTCP channel
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpStatistics {
	
	private static final Logger logger = Logger.getLogger(RtpStatistics.class);

	/** Default session bandwidth (in octets per second). Matches g.711: 64kbps */
	public static final int RTP_DEFAULT_BW = 8000;

	/** Fraction of the session bandwidth added for RTCP */
	public static final double RTCP_BW_FRACTION = 0.05;

	/** Default value for the RTCP bandwidth */
	public static final double RTCP_DEFAULT_BW = RTP_DEFAULT_BW * RTCP_BW_FRACTION;

	/** Fraction of the RTCP bandwidth to be shared among active senders. */
	public static final double RTCP_SENDER_BW_FRACTION = 0.25;

	/** Fraction of the RTCP bandwidth to be shared among receivers. */
	public static final double RTCP_RECEIVER_BW_FRACTION = 1 - RTCP_SENDER_BW_FRACTION;

	/** "timer reconsideration": converges to a value below the intended average */
	private static final double RTCP_COMPENSATION = Math.E - (3 / 2);

	/** Default value for the average RTCP packet size */
	public static final int RTCP_DEFAULT_AVG_SIZE = 200;

	/** The minimum interval between transmissions of compound RTCP packets. */
	public static final double RTCP_MIN_TIME = 5.0;

	/** Initial delay imposed before the first compound RTCP packet is sent. */
	public static final double INITIAL_RTCP_MIN_TIME = RTCP_MIN_TIME / 2;

	/* Core */
	private final RtpClock rtpClock;
	private final Clock wallClock;
	private final Random random;

	/* SSRC Data */
	private long ssrc;
	private String cname;

	/* Global RTP statistics */
	private long rtpLastHeartbeat;
	private volatile long rtpRxPackets;
	private volatile long rtpRxOctets;
	private volatile long rtpTxPackets;
	private volatile long rtpTxOctets;
	private volatile long rtpReceivedOn;
	private volatile long rtpSentOn;

	/* Global RTCP statistics */
	private RtcpPacketType rtcpNextPacketType;
	private double rtcpBw;
	private double rtcpAvgSize;
	private boolean weSent;

	/**
	 * Calculation of the RTCP packet interval depends upon an estimate of the
	 * number of sites participating in the session. New sites are added to the
	 * count when they are heard, and an entry for each SHOULD be created in a
	 * table indexed by the SSRC or CSRC identifier to keep track of them.
	 * 
	 * Entries MAY be deleted from the table when an RTCP BYE packet with the
	 * corresponding SSRC identifier is received, except that some straggler
	 * data packets might arrive after the BYE and cause the entry to be
	 * recreated. Instead, the entry SHOULD be marked as having received a BYE
	 * and then deleted after an appropriate delay.
	 * 
	 * A participant MAY mark another site inactive, or delete it if not yet
	 * valid, if no RTP or RTCP packet has been received for a small number of
	 * RTCP report intervals (5 is RECOMMENDED). This provides some robustness
	 * against packet loss.
	 */
	private final Map<Long, Member> membersMap;
	private int pmembers;
	private int members;
	
	private final List<Long> sendersList;
	private int senders;

	public RtpStatistics(final RtpClock clock, final String cname) {
		// Common
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();
		this.ssrc = System.currentTimeMillis();
		this.cname = cname;
		this.random = new Random();

		// RTP statistics
		this.rtpLastHeartbeat = 0;
		this.rtpRxPackets = 0;
		this.rtpTxPackets = 0;
		this.rtpReceivedOn = 0;
		this.rtpSentOn = 0;

		// RTCP statistics
		this.senders = 0;
		this.sendersList = new ArrayList<Long>();
		this.pmembers = 1;
		this.members = 1;
		this.membersMap = new HashMap<Long, Member>();
		this.membersMap.put(Long.valueOf(this.ssrc), new Member(this.rtpClock, this.ssrc));
		this.rtcpBw = RTP_DEFAULT_BW * RTCP_BW_FRACTION;
		this.rtcpAvgSize = RTCP_DEFAULT_AVG_SIZE;
		this.rtcpNextPacketType = RtcpPacketType.RTCP_REPORT;
		this.weSent = false;
	}
	
	public RtpStatistics(final RtpClock clock) {
		this(clock, "");
	}

	/**
	 * Gets the relative time since an RTP packet or Heartbeat was received.
	 * 
	 * @return The last heartbeat timestamp, in nanoseconds
	 */
	public long getLastHeartbeat() {
		return rtpLastHeartbeat;
	}

	/**
	 * Sets the relative time for the last received Heartbeat on a RTP Channel.<br>
	 * Used for RTP timeout control, not RTCP statistics.
	 * 
	 * @param rtpKeepAlive
	 *            The heartbeat timestamp, in nanoseconds.
	 */
	public void setLastHeartbeat(long rtpKeepAlive) {
		this.rtpLastHeartbeat = rtpKeepAlive;
	}

	/**
	 * Gets the RTP time stamp equivalent to the current time of the Wall Clock.
	 * 
	 * @return The current time stamp in RTP format.
	 */
	public long getRtpTime() {
		return this.wallClock.getTime();
	}
	
	/**
	 * Gets the current time of the Wall Clock.<br>
	 * 
	 * @return The current time of the wall clock, in milliseconds.
	 */
	public long getCurrentTime() {
		return this.wallClock.getCurrentTime();
	}

	/**
	 * Gets the SSRC of the RTP Channel
	 * 
	 * @return The SSRC identifier of the channel
	 */
	public long getSsrc() {
		return ssrc;
	}

	public void setSsrc(long ssrc) {
		// TODO check specs to know what to do when the SSRC changes
		this.ssrc = ssrc;
	}

	/**
	 * Gets the CNAME that identifies this source
	 * 
	 * @return The CNAME of the source
	 */
	public String getCname() {
		return cname;
	}

	/**
	 * Sets the CNAME that identifies this source
	 * 
	 * @param cname
	 *            The CNAME of the source
	 */
	public void setCname(String cname) {
		this.cname = cname;
	}

	/*
	 * RTP Statistics
	 */
	/**
	 * Gets the total number of RTP packets that were received during the RTP
	 * session.
	 * 
	 * @return The number of RTP packets
	 */
	public long getRtpPacketsReceived() {
		return rtpRxPackets;
	}

	/**
	 * 
	 * @return
	 */
	public long getRtpOctetsReceived() {
		return rtpRxOctets;
	}

	public long getRtpPacketsSent() {
		return rtpTxPackets;
	}

	public long getRtpOctetsSent() {
		return rtpTxOctets;
	}

	/**
	 * Gets the relative timestamp of the last received RTP packet.
	 * 
	 * @return The elapsed time, in nanoseconds.
	 */
	public long getRtpReceivedOn() {
		return rtpReceivedOn;
	}

	/**
	 * Gets the relative timestamp of the last transmitted RTP packet.
	 * 
	 * @return The elapsed time, in nanoseconds.
	 */
	public long getRtpSentOn() {
		return rtpSentOn;
	}

	/*
	 * RTCP Statistics
	 */
	/**
	 * Checks whether the application has sent data since the 2nd previous RTCP
	 * report was sent.
	 * 
	 * @return Whether data has been sent recently
	 */
	public boolean hasSent() {
		return this.weSent;
	}

	/**
	 * Gets the total RTCP bandwidth of this session.
	 * 
	 * @return The bandwidth, in octets per second
	 */
	public double getRtcpBw() {
		return rtcpBw;
	}

	/**
	 * Sets the total RTCP bandwidth of this session.
	 * 
	 * @param rtcpBw
	 *            The bandwidth, in octets per second
	 */
	public void setRtcpBw(double rtcpBw) {
		this.rtcpBw = rtcpBw;
	}

	/**
	 * Gets the type of RTCP packet that is scheduled to be transmitted next.
	 * 
	 * @return The type of the packet
	 */
	public RtcpPacketType getNextPacketType() {
		return rtcpNextPacketType;
	}

	/**
	 * Sets the type of RTCP packet that is scheduled to be transmitted next.
	 * 
	 * @param packetType
	 *            The type of the packet
	 */
	public void setRtcpPacketType(RtcpPacketType packetType) {
		this.rtcpNextPacketType = packetType;
	}

	/**
	 * Gets the most current estimate for the number of senders in the session
	 * 
	 * @return The estimate number of senders
	 */
	public int getSenders() {
		return this.senders;
	}

	public boolean isSender(long ssrc) {
		synchronized (this.sendersList) {
			return this.sendersList.contains(Long.valueOf(ssrc));
		}
	}

	public void addSender(long ssrc) {
		synchronized (this.sendersList) {
			if (!this.sendersList.contains(Long.valueOf(ssrc))) {
				this.sendersList.add(Long.valueOf(ssrc));
				this.senders++;
				if (this.ssrc == ssrc) {
					this.weSent = true;
				}
			}
		}
	}

	public void removeSender(long ssrc) {
		synchronized (this.sendersList) {
			if (this.sendersList.remove(Long.valueOf(ssrc))) {
				this.senders--;
				if (this.ssrc == ssrc) {
					this.weSent = false;
				}
			}
		}
	}

	public void clearSenders() {
		synchronized (this.sendersList) {
			this.sendersList.clear();
			this.senders = 0;
			this.weSent = false;
		}
	}

	/**
	 * Gets the estimated number of session members at the time <code>tn</code>
	 * was last recomputed.
	 * 
	 * @return The number of members
	 */
	public int getPmembers() {
		return pmembers;
	}

	/**
	 * Gets the most current estimate for the number of session members.
	 * 
	 * @return The number of members
	 */
	public int getMembers() {
		return members;
	}

	public Member getMember(long ssrc) {
		synchronized (this.membersMap) {
			return this.membersMap.get(Long.valueOf(ssrc));
		}
	}

	public List<Long> getMembersList() {
		List<Long> copy;
		synchronized (this.membersMap) {
			copy = new ArrayList<Long>(this.membersMap.keySet());
		}
		return copy;
	}

	public boolean isMember(long ssrc) {
		synchronized (this.membersMap) {
			return this.membersMap.containsKey(Long.valueOf(ssrc));
		}
	}

	public Member addMember(long ssrc) {
		Member member = getMember(ssrc);
		if (member == null) {
			synchronized (this.membersMap) {
				this.membersMap.put(Long.valueOf(ssrc), new Member(
						this.rtpClock, ssrc));
				this.members++;
			}
		}
		return member;
	}

	public void removeMember(long ssrc) {
		synchronized (this.membersMap) {
			if (this.membersMap.remove(Long.valueOf(ssrc)) != null) {
				this.members--;
			}
		}
	}

	/**
	 * Sets the estimate number of members (pmembers) equal to the number of
	 * currently registered members.
	 */
	public void confirmMembers() {
		this.pmembers = this.members;
	}

	public void resetMembers() {
		synchronized (this.membersMap) {
			this.membersMap.clear();
			this.membersMap.put(Long.valueOf(this.ssrc), new Member(this.rtpClock, this.ssrc));
			this.members = 1;
			this.pmembers = 1;
		}
	}

	/**
	 * Gets the average compound RTCP packet size.
	 * 
	 * @return The average packet size, in octets
	 */
	public double getRtcpAvgSize() {
		return rtcpAvgSize;
	}
	
	public void setRtcpAvgSize(double avgSize) {
		this.rtcpAvgSize = avgSize;
	}

	private double calculateAvgRtcpSize(double packetSize) {
		this.rtcpAvgSize = (1 / 16) * packetSize + (15 / 16) * this.rtcpAvgSize;
		return this.rtcpAvgSize;
	}

	/**
	 * 6.3.1 - Computing the RTCP Transmission Interval
	 * 
	 * To maintain scalability, the average interval between packets from a
	 * session participant should scale with the group size. This interval is
	 * called the calculated interval. It is obtained by combining a number of
	 * the pieces of state described above. The calculated interval T is then
	 * determined as follows:
	 * 
	 * 1. If the number of senders is less than or equal to 25% of the
	 * membership (members), the interval depends on whether the participant is
	 * a sender or not (based on the value of we_sent). If the participant is a
	 * sender (we_sent true), the constant C is set to the average RTCP packet
	 * size (avg_rtcp_size) divided by 25% of the RTCP bandwidth (rtcp_bw), and
	 * the constant n is set to the number of senders. If we_sent is not true,
	 * the constant C is set to the average RTCP packet size divided by 75% of
	 * the RTCP bandwidth. The constant n is set to the number of receivers
	 * (members - senders). If the number of senders is greater than 25%,
	 * senders and receivers are treated together. The constant C is set to the
	 * average RTCP packet size divided by the total RTCP bandwidth and n is set
	 * to the total number of members. As stated in Section 6.2, an RTP profile
	 * MAY specify that the RTCP bandwidth may be explicitly defined by two
	 * separate parameters (call them S and R) for those participants which are
	 * senders and those which are not. In that case, the 25% fraction becomes
	 * S/(S+R) and the 75% fraction becomes R/(S+R). Note that if R is zero, the
	 * percentage of senders is never greater than S/(S+R), and the
	 * implementation must avoid division by zero.
	 * 
	 * 2. If the participant has not yet sent an RTCP packet (the variable
	 * initial is true), the constant Tmin is set to 2.5 seconds, else it is set
	 * to 5 seconds.
	 * 
	 * 3. The deterministic calculated interval Td is set to max(Tmin, n*C).
	 * 
	 * 4. The calculated interval T is set to a number uniformly distributed
	 * between 0.5 and 1.5 times the deterministic calculated interval.
	 * 
	 * 5. The resulting value of T is divided by e-3/2=1.21828 to compensate for
	 * the fact that the timer reconsideration algorithm converges to a value of
	 * the RTCP bandwidth below the intended average.
	 * 
	 * This procedure results in an interval which is random, but which, on
	 * average, gives at least 25% of the RTCP bandwidth to senders and the rest
	 * to receivers. If the senders constitute more than one quarter of the
	 * membership, this procedure splits the bandwidth equally among all
	 * participants, on average.
	 * 
	 * @return the new transmission interval, in milliseconds
	 */
	public long rtcpInterval(boolean initial) {
		return rtcpInterval(this.weSent, initial);
	}

	/**
	 * Calculates the RTCP interval for a receiver, that is without the
	 * randomization factor (we_sent=false).
	 * 
	 * @param initial
	 * @return the new transmission interval, in milliseconds
	 */
	public long rtcpReceiverInterval(boolean initial) {
		return rtcpInterval(false, initial);
	}

	private long rtcpInterval(boolean weSent, boolean initial) {
		// 1 - calculate n and c
		double c;
		int n;
		if (this.senders <= (this.members * RTCP_SENDER_BW_FRACTION)) {
			if (this.weSent) {
				c = this.rtcpAvgSize / (RTCP_SENDER_BW_FRACTION * this.rtcpBw);
				n = this.senders;
			} else {
				c = this.rtcpAvgSize / (RTCP_RECEIVER_BW_FRACTION * this.rtcpBw);
				n = this.members - this.senders;
			}
		} else {
			c = this.rtcpAvgSize / this.rtcpBw;
			n = this.members;
		}

		// 2 - calculate Tmin
		double tMin = initial ? INITIAL_RTCP_MIN_TIME : RTCP_MIN_TIME;

		// 3 - calculate Td
		double td = Math.max(tMin, n * c);

		// 4 - calculate interval T
		double min = td * 0.5;
		double max = td * 1.5;
		double t = min + (max - min) * this.random.nextDouble();

		// 5 - divide T by e-3/2 and convert to milliseconds
		return (long) ((t / RTCP_COMPENSATION) * 1000);
	}

	/**
	 * Checks whether this SSRC is still a sender.
	 * 
	 * If an RTP packet has not been transmitted since time tc - 2T, the
	 * participant removes itself from the sender table, decrements the sender
	 * count, and sets we_sent to false.
	 * 
	 * @return whether this SSRC is still considered a sender
	 */
	public boolean isSenderTimeout() {
		long t = rtcpReceiverInterval(false);
		long minTime = getCurrentTime() - (2 * t);

		if (this.rtpSentOn < minTime) {
			removeSender(this.ssrc);
		}
		logger.info("Are we sender? "+ weSent);
		return this.weSent;
	}

	public void reset() {
		// TODO finish reset for RTCP statistics
		this.rtpRxPackets = 0;
		this.rtpTxPackets = 0;
	}

	/*
	 * EVENTS
	 */
	public void onRtpSent(RtpPacket packet) {
		this.rtpTxPackets++;
		this.rtpTxOctets += packet.getLength();
		this.rtpSentOn = this.wallClock.getCurrentTime();
		/*
		 * If the participant sends an RTP packet when we_sent is false, it adds
		 * itself to the sender table and sets we_sent to true.
		 */
		if (!this.weSent) {
			addSender(Long.valueOf(this.ssrc));
		}
	}

	public void onRtpReceive(RtpPacket packet) {
		// Increment global statistics
		this.rtpRxPackets++;
		this.rtpRxOctets += packet.getLength();
		this.rtpReceivedOn = this.wallClock.getTime();

		// Increment member statistics
		long syncSource = packet.getSyncSource();
		Member member = getMember(syncSource);

		if (member == null) {
			member = addMember(syncSource);
		}
		member.onReceiveRtp(packet);
	}
	
	public void onRtcpSent(RtcpPacket packet) {
		calculateAvgRtcpSize(packet.getSize());
	}
	
	public void onRtcpReceive(RtcpPacket rtcpPacket) {
		/*
		 * All RTCP packets MUST be sent in a compound packet of at least two
		 * individual packets. The first RTCP packet in the compound packet MUST
		 * always be a report packet to facilitate header validation
		 */
		RtcpReport report = rtcpPacket.getReport();
		long ssrc = report.getSsrc();

		/*
		 * What we do depends on whether we have left the group, and are waiting
		 * to send a BYE or an RTCP report.
		 */
		switch (rtcpPacket.getPacketType()) {
		case RTCP_REPORT:

			/*
			 * When an RTP or (non-bye) RTCP packet is received from a
			 * participant whose SSRC is not in the member table, the SSRC is
			 * added to the table, and the value for members is updated once the
			 * participant has been validated.
			 */
			Member member = getMember(ssrc);
			if (member == null && RtcpPacketType.RTCP_REPORT.equals(this.rtcpNextPacketType)) {
				member = addMember(ssrc);
			}
			
			// Receiving an SR has impact on the statistics of the member
			if(report.isSender()) {
				member.onReceiveSR((RtcpSenderReport) report);
			}

			break;
		case RTCP_BYE:

			switch (this.rtcpNextPacketType) {
			case RTCP_REPORT:

				/*
				 * If the received packet is an RTCP BYE packet, the SSRC is
				 * checked against the member table. If present, the entry is
				 * removed from the table, and the value for members is updated.
				 */
				if (isMember(ssrc)) {
					removeMember(ssrc);
				}

				/*
				 * The SSRC is then checked against the sender table. If
				 * present, the entry is removed from the table, and the value
				 * for senders is updated.
				 */
				if (isSender(ssrc)) {
					removeSender(ssrc);
				}
				break;

			case RTCP_BYE:

				/*
				 * Every time a BYE packet from another participant is received,
				 * members is incremented by 1 regardless of whether that
				 * participant exists in the member table or not, and when SSRC
				 * sampling is in use, regardless of whether or not the BYE SSRC
				 * would be included in the sample.
				 * 
				 * members is NOT incremented when other RTCP packets or RTP
				 * packets are received, but only for BYE packets. Similarly,
				 * avg_rtcp_size is updated only for received BYE packets.
				 * senders is NOT updated when RTP packets arrive; it remains 0.
				 */
				this.members++;
				break;

			default:
				logger.warn("Unknown type of scheduled event: " + this.rtcpNextPacketType.name());
				break;
			}
			break;
		default:
			logger.warn("Unkown RTCP packet type: " + rtcpPacket.getPacketType().name() + ". Dropping packet.");
			break;
		}

		// For each RTCP packet received, the value of avg_rtcp_size is updated.
		calculateAvgRtcpSize(rtcpPacket.getSize());
	}
	
}
