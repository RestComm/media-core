/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.mobicents.media.server.impl.rtp.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpIntervalCalculator;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;
import org.mobicents.media.server.impl.rtcp.RtcpPacketType;
import org.mobicents.media.server.impl.rtcp.RtcpReport;
import org.mobicents.media.server.impl.rtcp.RtcpReportBlock;
import org.mobicents.media.server.impl.rtcp.RtcpSdes;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtp.CnameGenerator;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.SsrcGenerator;
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
	public static final double RTCP_RECEIVER_BW_FRACTION = 1.0 - RTCP_SENDER_BW_FRACTION;

	/** Default value for the average RTCP packet size */
	public static final double RTCP_DEFAULT_AVG_SIZE = 200.0;

	/* Core */
	private final RtpClock rtpClock;
	private final Clock wallClock;

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
	private volatile long rtpTimestamp;

	/* Global RTCP statistics */
	private RtcpPacketType rtcpNextPacketType;
	private double rtcpBw;
	private double rtcpAvgSize;
	private boolean weSent;
	
	private volatile long rtcpTxPackets;
	private volatile long rtcpTxOctets;

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
	private final Map<Long, RtpMember> membersMap;
	private int pmembers;
	private int members;
	
	private final List<Long> sendersList;
	private int senders;

	public RtpStatistics(final RtpClock clock, final long ssrc, final String cname) {
		// Common
		this.rtpClock = clock;
		this.wallClock = clock.getWallClock();
		this.ssrc = ssrc;
		this.cname = cname;

		// RTP statistics
		this.rtpLastHeartbeat = 0;
		this.rtpRxPackets = 0;
		this.rtpRxOctets = 0;
		this.rtpTxPackets = 0;
		this.rtpTxOctets = 0;
		this.rtpReceivedOn = 0;
		this.rtpSentOn = 0;
		this.rtpTimestamp = -1;

		// RTCP statistics
		this.senders = 0;
		this.sendersList = new ArrayList<Long>();
		this.pmembers = 1;
		this.members = 1;
		this.membersMap = new HashMap<Long, RtpMember>();
		this.membersMap.put(Long.valueOf(this.ssrc), new RtpMember(this.rtpClock, this.ssrc));
		this.rtcpBw = RTP_DEFAULT_BW * RTCP_BW_FRACTION;
		this.rtcpAvgSize = RTCP_DEFAULT_AVG_SIZE;
		this.rtcpNextPacketType = RtcpPacketType.RTCP_REPORT;
		this.weSent = false;

		this.rtcpTxPackets = 0;
		this.rtcpTxOctets = 0;
	}
	
	public RtpStatistics(final RtpClock clock, final long ssrc) {
		this(clock, ssrc, "");
	}
	
	public RtpStatistics(final RtpClock clock) {
		this(clock, SsrcGenerator.generateSsrc(), CnameGenerator.generateCname());
	}
	
	public void setSsrc(long ssrc) {
		this.ssrc = ssrc;
	}
	
	public void setCname(String cname) {
		this.cname = cname;
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
	public long getTime() {
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
	
	public long getRtpTime(long time) {
		return this.rtpClock.convertToRtpTime(time);
	}

	/**
	 * Gets the SSRC of the RTP Channel
	 * 
	 * @return The SSRC identifier of the channel
	 */
	public long getSsrc() {
		return ssrc;
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
    public int getAverageLatency() {
        int latency = 0;
        if (this.members > 0) {
            for (RtpMember member : this.membersMap.values()) {
                latency += member.getRTT();
            }
            latency /= this.members;
        }
        return latency;
    }
	
	/**
     * Gets the total number of RTP packets, from all registered members, that were lost during RTP session.
     * 
     * @return the number of lost RTP packets
     */
    public long getRtpPacketsLost() {
        long lost = 0L;
        for (RtpMember member : this.membersMap.values()) {
            lost += member.getPacketsLost();
        }
        return lost;
    }
	
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
	
	/**
	 * Gets ths time stamp of the last RTP packet sent.
	 * 
	 * @return The time stamp of the packet.
	 */
	public long getRtpTimestamp() {
		return rtpTimestamp;
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
	 * Gets the type of RTCP packet that is scheduled to be transmitted next.
	 * 
	 * @return The type of the packet
	 */
	public RtcpPacketType getRtcpPacketType() {
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

	private void addSender(long ssrc) {
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

	private void removeSender(long ssrc) {
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

	public RtpMember getMember(long ssrc) {
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

	private RtpMember addMember(long ssrc) {
		return addMember(ssrc, "");
	}
	
	private RtpMember addMember(long ssrc, String cname) {
		RtpMember member = getMember(ssrc);
		if (member == null) {
			synchronized (this.membersMap) {
				member = new RtpMember(this.rtpClock, ssrc, cname);
				this.membersMap.put(Long.valueOf(ssrc), member);
				this.members++;
			}
		}
		return member;
	}

	private void removeMember(long ssrc) {
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
			this.membersMap.put(Long.valueOf(this.ssrc), new RtpMember(this.rtpClock, this.ssrc));
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
		this.rtcpAvgSize = (1.0 / 16.0) * packetSize + (15.0 / 16.0) * this.rtcpAvgSize;
		return this.rtcpAvgSize;
	}
	
	public long getRtcpPacketsSent() {
		return rtcpTxPackets;
	}

	public long getRtcpOctetsSent() {
		return rtcpTxOctets;
	}

	/**
	 * Calculates a random interval to transmit the next RTCP report, according
	 * to <a
	 * href="http://tools.ietf.org/html/rfc3550#section-6.3.1">RFC3550</a>.
	 * 
	 * @param initial
	 *            Whether an RTCP packet was already sent or not. Usually the
	 *            minimum interval for the first packet is lower than the rest.
	 * 
	 * @return the new transmission interval, in milliseconds
	 */
	public long rtcpInterval(boolean initial) {
		return RtcpIntervalCalculator.calculateInterval(initial, weSent,
				senders, members, rtcpAvgSize, rtcpBw, RTCP_BW_FRACTION,
				RTCP_SENDER_BW_FRACTION, RTCP_RECEIVER_BW_FRACTION);
	}

	/**
	 * Calculates the RTCP interval for a receiver, that is without the
	 * randomization factor (we_sent=false), according to <a
	 * href="http://tools.ietf.org/html/rfc3550#section-6.3.1">RFC3550</a>.
	 * 
	 * @param initial
	 *            Whether an RTCP packet was already sent or not. Usually the
	 *            minimum interval for the first packet is lower than the rest.
	 * @return the new transmission interval, in milliseconds
	 */
	public long rtcpReceiverInterval(boolean initial) {
		return RtcpIntervalCalculator.calculateInterval(initial, false,
				senders, members, rtcpAvgSize, rtcpBw, RTCP_BW_FRACTION,
				RTCP_SENDER_BW_FRACTION, RTCP_RECEIVER_BW_FRACTION);
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
		return this.weSent;
	}

	public void reset() {
		// Common
		this.ssrc = SsrcGenerator.generateSsrc();
		this.cname = CnameGenerator.generateCname();
		
		// RTP statistics
		this.rtpLastHeartbeat = 0;
		this.rtpRxPackets = 0;
		this.rtpTxPackets = 0;
		this.rtpReceivedOn = 0;
		this.rtpSentOn = 0;
		this.rtpTimestamp = -1;

		// RTCP statistics
		this.senders = 0;
		this.sendersList.clear();
		this.pmembers = 1;
		this.members = 1;
		this.membersMap.clear();
		this.membersMap.put(Long.valueOf(this.ssrc), new RtpMember(this.rtpClock, this.ssrc));
		this.rtcpBw = RTP_DEFAULT_BW * RTCP_BW_FRACTION;
		this.rtcpAvgSize = RTCP_DEFAULT_AVG_SIZE;
		this.rtcpNextPacketType = RtcpPacketType.RTCP_REPORT;
		this.weSent = false;
	}

	/*
	 * EVENTS
	 */
	public void onRtpSent(RtpPacket packet) {
		this.rtpTxPackets++;
		this.rtpTxOctets += packet.getPayloadLength();
		this.rtpSentOn = this.wallClock.getCurrentTime();
		this.rtpTimestamp = packet.getTimestamp();
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
		this.rtpRxOctets += packet.getPayloadLength();
		this.rtpReceivedOn = this.wallClock.getTime();
		
		// Note that there is no point in registering new members if RTCP handler has scheduled a BYE
		if(RtcpPacketType.RTCP_REPORT.equals(this.rtcpNextPacketType)) {
			long syncSource = packet.getSyncSource();

			/*
			 * When an RTP packet is received from a participant whose SSRC is
			 * not in the sender table, the SSRC is added to the table, and the
			 * value for senders is updated.
			 */
			RtpMember member = getMember(syncSource);
			
			if (member == null) {
				member = addMember(syncSource);
			}

			if (!isSender(syncSource)) {
				addSender(syncSource);
			}
			
			// Update member statistics
			member.onReceiveRtp(packet);
		}
	}
	
	public void onRtcpSent(RtcpPacket packet) {
		calculateAvgRtcpSize(packet.getSize());
		this.rtcpTxPackets++;
		this.rtcpTxOctets += packet.getSize();
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
			 * 
			 * Don't bother registering members if an RTCP BYE is scheduled!
			 */
			RtpMember member = getMember(ssrc);
			if (member == null && RtcpPacketType.RTCP_REPORT.equals(this.rtcpNextPacketType)) {
				RtcpSdes sdes = rtcpPacket.getSdes();
				String cname = sdes == null ? "" : sdes.getCname();
				member = addMember(ssrc, cname);
			}
			
			if(rtcpPacket.isSender() && member != null) {
				// Receiving an SR has impact on the statistics of the member
				member.onReceiveSR((RtcpSenderReport) report);

				// estimate round trip delay
				RtcpReportBlock reportBlock = report.getReportBlock(this.ssrc);
				if(reportBlock!= null) {
					member.estimateRtt(this.wallClock.getCurrentTime(), reportBlock.getLsr(), reportBlock.getDlsr());
				}
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
