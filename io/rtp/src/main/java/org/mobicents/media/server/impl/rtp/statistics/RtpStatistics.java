package org.mobicents.media.server.impl.rtp.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.mobicents.media.server.impl.rtcp.RtcpPacketType;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.scheduler.Clock;

/**
 * Encapsulates statistics of an RTP/RTCP channel
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpStatistics {
	
	/* Constants */
	/** Default session bandwidth (in octets per second). Matches g.711 bandwith: 64kbps */
	public static final int RTP_DEFAULT_BW = 8000;
	
	/**
	 * The control traffic should be limited to a small and known fraction of
	 * the session bandwidth: small so that the primary function of the
	 * transport protocol to carry data is not impaired [...]
	 * 
	 * It is RECOMMENDED that the fraction of the session bandwidth added for
	 * RTCP be fixed at 5%.
	 */
	public static final double RTCP_BW_FRACTION = 0.05;

	/** Default value for the RTCP bandwith */
	public static final double RTCP_DEFAULT_BW = RTP_DEFAULT_BW * RTCP_BW_FRACTION;
	
	/**
	 * Fraction of the RTCP bandwidth to be shared among active senders.
	 * 
	 * (This fraction was chosen so that in a typical session with one or two
	 * active senders, the computed report time would be roughly equal to the
	 * minimum report time so that we don't unnecessarily slow down receiver
	 * reports.)
	 */
	public static final double RTCP_SENDER_BW_FRACTION = 0.25;

	/** Fraction of the RTCP bandwidth to be shared among receivers. */
	public static final double RTCP_RECEIVER_BW_FRACTION = 1- RTCP_SENDER_BW_FRACTION;
	
	/** To compensate for "timer reconsideration" converging to a value below the intended average. */
	private static final double RTCP_COMPENSATION = Math.E - (3 / 2);

	/** Default value for the average RTCP packet size */
	public static final int RTCP_DEFAULT_AVG_SIZE = 200;
	
	/**
	 * The calculated interval (in ms) between transmissions of compound RTCP
	 * packets SHOULD also have a lower bound to avoid having bursts of packets
	 * exceed the allowed bandwidth when the number of participants is small and
	 * the traffic isn't smoothed according to the law of large numbers.
	 * 
	 * It also keeps the report interval from becoming too small during
	 * transient outages like a network partition such that adaptation is
	 * delayed when the partition heals.
	 * 
	 * The RECOMMENDED value for a fixed minimum interval is 5 seconds.
	 */
	public static final double RTCP_MIN_TIME = 5.0;

	/**
	 * At application startup, a delay SHOULD be imposed before the first
	 * compound RTCP packet is sent to allow time for RTCP packets to be
	 * received from other participants so the report interval will converge to
	 * the correct value more quickly.
	 * 
	 * This delay MAY be set to half the minimum interval to allow quicker
	 * notification that the new participant is present.
	 */
	public static final double INITIAL_RTCP_MIN_TIME = RTCP_MIN_TIME / 2;
	
	/* Common */
	private long ssrc;
	private final String cname;
	
	private final RtpClock rtpClock;
	private final Clock wallClock;
	private final Random random;

	/* RTP statistics */
	private long lastHeartbeat;
	
	private volatile long rtpRxPackets;
	private volatile long rtpRxOctets;
	
	private volatile long rtpTxPackets;
	private volatile long rtpTxOctets;
	
	private int rtpSeqNum;
	
	/** Relative timestamp of the last received RTP packet in nanoseconds */
	private volatile long rtpReceivedOn;
	
	/** Relative timestamp of the last transmitted RTP packet in nanoseconds */
	private volatile long rtpSentOn;

	/* RTCP statistics */
	/** The type of RTCP packet that is scheduled to be transmitted next. */
	private RtcpPacketType scheduledPacketType;
	
	/**
	 * The total bandwidth that will be used for RTCP packets by all members of this session, in octets per second.
	 */
	private double rtcpBw;
	
	/** List of SSRC that are senders */
	private final List<Long> sendersList;

	/** The most current estimate for the number of senders in the session */
	private int senders;

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
	private final Map<Long, Member> membersList;

	/** The estimated number of session members at the time <code>tn</code> was last recomputed */
	private int pmembers;

	/** the most current estimate for the number of session members */
	private int members;
	
	/** The average compound RTCP packet size, in octets */
	private double avgRtcpSize;
	
	/** Flag that is true if the application has sent data since the 2nd previous RTCP report was transmitted */
	private boolean weSent;
	
	public RtpStatistics(final RtpClock clock, final String cname) {
		this.lastHeartbeat = 0;
		
		// Common
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();
		this.ssrc = System.currentTimeMillis();
		this.cname = cname;
		this.random = new Random();

		// RTP statistics
		this.rtpRxPackets = 0;
		this.rtpTxPackets = 0;
		this.rtpSeqNum = 0;
		this.rtpReceivedOn = 0;
		this.rtpSentOn = 0;

		// RTCP statistics
		this.senders = 0;
		this.sendersList = new ArrayList<Long>();
		this.pmembers = 1;
		this.members = 1;
		this.membersList = new HashMap<Long, Member>();
		this.membersList.put(Long.valueOf(this.ssrc), new Member(this.rtpClock, this.ssrc));
		this.rtcpBw = RTP_DEFAULT_BW * RTCP_BW_FRACTION;
		this.avgRtcpSize = RTCP_DEFAULT_AVG_SIZE;
		this.scheduledPacketType = RtcpPacketType.RTCP_REPORT;
		this.weSent = false;
	}
	
	/**
	 * Gets the relative time since an RTP packet or Heartbeat was received.
	 * 
	 * @return The last heartbeat timestamp, in nanoseconds
	 */
	public long getLastHeartbeat() {
		return lastHeartbeat;
	}

	/**
	 * Sets the relative time for the last received Heartbeat on a RTP Channel.<br>
	 * Used for RTP timeout control, not RTCP statistics.
	 * 
	 * @param rtpKeepAlive
	 *            The heartbeat timestamp, in nanoseconds.
	 */
	public void setLastHeartbeat(long rtpKeepAlive) {
		this.lastHeartbeat = rtpKeepAlive;
	}
	
	/**
	 * Gets the current time of the Wall Clock.<br>
	 * 
	 * @return The elapsed time of the wall clock, in nanoseconds.
	 */
	public long getCurrentTime() {
		return this.wallClock.getTime();
	}
	
	/**
	 * Gets the RTP timestamp equivalent to the current time of the Wall Clock.
	 * 
	 * @return The current timestamp in RTP format.
	 */
	public long getRtpTime() {
		return this.rtpClock.getLocalRtpTime();
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
	
	public void onRtpReceive(int packetSize) {
		this.rtpRxPackets++;
		this.rtpRxOctets += packetSize;
		this.rtpReceivedOn = this.wallClock.getTime();
	}

	public long getRtpPacketsSent() {
		return rtpTxPackets;
	}
	
	public long getRtpOctetsSent() {
		return rtpTxOctets;
	}

	public void onRtpSent(int packetSize) {
		this.rtpTxPackets++;
		this.rtpTxOctets += packetSize;
		this.rtpSentOn = this.wallClock.getTime();
		/*
		 * If the participant sends an RTP packet when we_sent is false, it adds
		 * itself to the sender table and sets we_sent to true.
		 */
		if (!this.weSent) {
			addSender(Long.valueOf(this.ssrc));
		}
	}

	public int getSequenceNumber() {
		return rtpSeqNum;
	}

	public int nextSequenceNumber() {
		this.rtpSeqNum++;
		return this.rtpSeqNum;
	}

	public long getRtpReceivedOn() {
		return rtpReceivedOn;
	}

	public void setRtpReceivedOn(long timestamp) {
		this.rtpReceivedOn = timestamp;
	}
	
	public long getRtpSentOn() {
		return rtpSentOn;
	}

	/*
	 * RTCP Statistics
	 */
	public boolean hasSent() {
		return this.weSent;
	}
	
	public double getRtcpBw() {
		return rtcpBw;
	}
	
	public void setRtcpBw(double rtcpBw) {
		this.rtcpBw = rtcpBw;
	}
	
	public RtcpPacketType getScheduledPacketType() {
		return scheduledPacketType;
	}
	
	public void setScheduledPacketType(RtcpPacketType scheduledPacketType) {
		this.scheduledPacketType = scheduledPacketType;
	}
	
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
				if(this.ssrc == ssrc) {
					this.weSent = true;
				}
			}
		}
	}

	public void removeSender(long ssrc) {
		synchronized (this.sendersList) {
			if (this.sendersList.remove(Long.valueOf(ssrc))) {
				this.senders--;
				if(this.ssrc == ssrc) {
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
	
	public int getPmembers() {
		return pmembers;
	}

	/**
	 * Sets the estimate number of members (pmembers) equal to the number of
	 * currently registered members.
	 */
	public void confirmMembers() {
		this.pmembers = this.members;
	}

	public int getMembers() {
		return members;
	}
	
	public List<Long> getMembersList() {
		List<Long> copy;
		synchronized (this.membersList) {
			copy = new ArrayList<Long>(this.membersList.keySet());
		}
		return copy;
	}

	public boolean isMember(long ssrc) {
		synchronized (this.membersList) {
			return this.membersList.containsKey(Long.valueOf(ssrc));
		}
	}

	public void addMember(long ssrc) {
		if (!isMember(ssrc)) {
			synchronized (this.membersList) {
				this.membersList.put(Long.valueOf(ssrc), new Member(this.rtpClock, ssrc));
				this.members++;
			}
		}
	}
	
	public void addMember() {
		this.members++;
	}

	public void removeMember(long ssrc) {
		synchronized (this.membersList) {
			if (this.membersList.remove(Long.valueOf(ssrc)) != null) {
				this.members--;
			}
		}
	}
	
	public void resetMembers() {
		synchronized (this.membersList) {
			this.membersList.clear();
			this.membersList.put(Long.valueOf(this.ssrc), new Member(this.rtpClock, this.ssrc));
			this.members = 1;
			this.pmembers = 1;
		}
	}
	
	public double getAvgRtcpSize() {
		return avgRtcpSize;
	}
	
	public void setAvgRtcpSize(double avgRtcpSize) {
		this.avgRtcpSize = avgRtcpSize;
	}
	
	public double calculateAvgRtcpSize(double packetSize) {
		this.avgRtcpSize = (1 / 16) * packetSize + (15 / 16) * this.avgRtcpSize;
		return this.avgRtcpSize;
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
	 * @return the new transmission interval, in nanoseconds
	 */
	public long rtcpInterval(boolean initial) {
		return rtcpInterval(this.weSent, initial);
	}
	
	/**
	 * Calculates the RTCP interval for a receiver, that is without the
	 * randomization factor (we_sent=false).
	 * 
	 * @param initial
	 * @return the new transmission interval, in nanoseconds
	 */
	public long rtcpReceiverInterval(boolean initial) {
		return rtcpInterval(false, initial);
	}
	
	private long rtcpInterval(boolean weSent, boolean initial) {
		// 1 - calculate n and c
		double c;
		int n;
		if (this.senders <= (this.members * RTCP_SENDER_BW_FRACTION)) {
			if(this.weSent) {
				c = this.avgRtcpSize / (RTCP_SENDER_BW_FRACTION * this.rtcpBw);
				n = this.senders;
			} else {
				c = this.avgRtcpSize / (RTCP_RECEIVER_BW_FRACTION * this.rtcpBw);
				n = this.members - this.senders;
			}
		} else {
			c = this.avgRtcpSize / this.rtcpBw;
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
		
		// 5 - divide T by e-3/2 and convert to nanoseconds
		return (long) ((t / RTCP_COMPENSATION) * 1000000000L);
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
		
		if(this.rtpSentOn < minTime) {
			removeSender(this.ssrc);
		}
		return this.weSent;
	}
	
	public void reset() {
		// TODO finish reset for RTCP statistics
		this.rtpRxPackets = 0;
		this.rtpTxPackets = 0;
	}

}
