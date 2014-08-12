package org.mobicents.media.server.impl.rtcp;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpStatistics;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandler implements PacketHandler {
	
	private static final Logger logger = Logger.getLogger(RtcpHandler.class);

	/*
	 * Core stuff
	 */
	private final Scheduler scheduler;
	private final Timer timer;
	private DatagramChannel channel;
	private ByteBuffer byteBuffer;
	private final RtcpPacket rtcpPacket;
	private final Random random;

	/*
	 * RTCP stuff
	 */
	/**
	 * Maximum number of report blocks that will fit in an SR or RR packet
	 */
	public static final int MAX_BLOCKS = 31;

	/**
	 * The control traffic should be limited to a small and known fraction of
	 * the session bandwidth: small so that the primary function of the
	 * transport protocol to carry data is not impaired [...]
	 * 
	 * It is RECOMMENDED that the fraction of the session bandwidth added for
	 * RTCP be fixed at 5%.
	 */
	public static final double RTCP_BW_FRACTION = 0.05;
	
	/**
	 * Fraction of the RTCP bandwidth to be shared among active senders.
	 * 
	 * (This fraction was chosen so that in a typical session with one or two
	 * active senders, the computed report time would be roughly equal to the
	 * minimum report time so that we don't unnecessarily slow down receiver
	 * reports.)
	 */
	public static final double RTCP_SENDER_BW_FRACTION = 0.25;

	/**
	 * Fraction of the RTCP bandwidth to be shared among receivers.
	 */
	public static final double RTCP_RECEIVER_BW_FRACTION = 1- RTCP_SENDER_BW_FRACTION;
	
	/**
	 * To compensate for "timer reconsideration" converging to a value below the
	 * intended average.
	 */
	public static final double COMPENSATION = Math.E - (3 / 2);

	/**
	 * Default value for the average RTCP packet size
	 */
	public static final int DEFAULT_AVG_SIZE = 200;

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

	/**
	 * Stores statistics regarding the RTP session
	 */
	private final RtpStatistics statistics;
	
	/**
	 * The scheduled task responsible for transmitting the RTCP packet.
	 */
	private ScheduledTask scheduledTask;

	/**
	 * the last time an RTCP packet was transmitted
	 */
	private double tp;

	/**
	 * the current time
	 */
	private double tc;

	/**
	 * the next scheduled transmission time of an RTCP packet
	 */
	private double tn;

	/**
	 * the estimated number of session members at the time <code>tn</code> was
	 * last recomputed
	 */
	private int pmembers;

	/**
	 * the most current estimate for the number of session members
	 */
	private int members;

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
	private final List<Long> membersList;

	/**
	 * The target RTCP bandwidth, i.e., the total bandwidth that will be used
	 * for RTCP packets by all members of this session, in octets per second.
	 * This will be a specified fraction of the "session bandwidth" parameter
	 * supplied to the application at startup
	 */
	private double rtcpBw;

	/**
	 * Flag that is true if the application has sent data since the 2nd previous
	 * RTCP report was transmitted
	 */
	private boolean weSent;

	/**
	 * The average compound RTCP packet size, in octets, over all RTCP packets
	 * sent and received by this participant. The size includes lower-layer
	 * transport and network protocol headers (e.g., UDP and IP)
	 */
	private double avgRtcpSize;

	/**
	 * Flag that is true if the application has not yet sent an RTCP packet
	 */
	private boolean initial;

	/**
	 * Upon joining the session, the participant initializes tp to 0, tc to 0,
	 * senders to 0, pmembers to 1, members to 1, we_sent to false, rtcp_bw to
	 * the specified fraction of the session bandwidth, initial to true, and
	 * avg_rtcp_size to the probable size of the first RTCP packet that the
	 * application will later construct.
	 * 
	 * The calculated interval T is then computed, and the first packet is
	 * scheduled for time tn = T. This means that a transmission timer is set
	 * which expires at time T. Note that an application MAY use any desired
	 * approach for implementing this timer.
	 * 
	 * The participant adds its own SSRC to the member table.
	 */
	public RtcpHandler(final Scheduler scheduler, final RtpStatistics statistics) {
		// core stuff
		this.scheduler = scheduler;
		this.timer = new Timer();
		this.rtcpPacket = new RtcpPacket();
		this.random = new Random();

		// rtcp stuff
		this.statistics = statistics;
		this.scheduledTask = null;
		this.tp = 0.0;
		this.tc = 0.0;
		this.pmembers = 1;
		this.members = 1;
		this.membersList = new ArrayList<Long>();
		this.weSent = false;
		this.rtcpBw = RtpChannel.DEFAULT_BW * RTCP_BW_FRACTION;
		this.initial = true;
		this.avgRtcpSize = DEFAULT_AVG_SIZE;
	}

	/**
	 * Computes the calculated interval T, and the first packet is scheduled for
	 * time tn = T. This means that a transmission timer is set which expires at
	 * time T.
	 * 
	 * The participant adds its own SSRC to the member table.
	 */
	public void joinRtpSession() {
		this.membersList.add(this.statistics.getSsrc());
		// TODO compute timestamp using algorithm explained in
		// http://tools.ietf.org/html/rfc3550#appendix-A.7
		this.tn = INITIAL_RTCP_MIN_TIME;
		schedule((long) (this.tn * 1000));
	}

	private boolean isRegistered(long ssrc, List<Long> list) {
		// Make a safe copy of current data
		List<Long> copy;
		synchronized (list) {
			copy = new ArrayList<Long>(list);
		}
		// search for the member
		return copy.contains(Long.valueOf(ssrc));
	}
	
	public boolean isMember(long ssrc) {
		return isRegistered(ssrc, this.membersList);
	}
	
	private void addMember(long ssrc) {
		synchronized (this.membersList) {
			this.membersList.add(Long.valueOf(ssrc));
			this.members++;
		}
	}
	
	private void removeMember(long ssrc) {
		synchronized (this.membersList) {
			if (this.membersList.remove(Long.valueOf(ssrc))) {
				this.members--;
			}
		}
	}
	
	/**
	 * Schedules an event to occur at a certain time.
	 * 
	 * @param timestamp The timestamp of the date when the event should be fired
	 */
	private void schedule(long timestamp) {
		if (weSent) {
			// TODO schedule SR
		} else {
			// TODO schedule RR
		}
	}
	
	/**
	 * Reschedules a previously scheduled event
	 * 
	 * @param task
	 *            The task to be re-scheduled
	 * @param timestamp
	 *            The new date of the event
	 */
	private void reschedule(ScheduledTask task, long timestamp) {
		task.cancel();
		task.setTimestamp(timestamp);
		this.timer.schedule(task, timestamp);
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
	 * @return the new transmission interval, in seconds
	 */
	protected double rtcpInterval() {
		// 1 - calculate n and c
		double c;
		int n;
		if (this.statistics.getSenders() <= (this.members * RTCP_SENDER_BW_FRACTION)) {
			if(weSent) {
				c = this.avgRtcpSize / (RTCP_SENDER_BW_FRACTION * this.rtcpBw);
				n = this.statistics.getSenders();
			} else {
				c = this.avgRtcpSize / (RTCP_RECEIVER_BW_FRACTION * this.rtcpBw);
				n = this.members - this.statistics.getSenders();
			}
		} else {
			c = this.avgRtcpSize / this.rtcpBw;
			n = this.members;
		}
		
		// 2 - calculate Tmin
		double tMin = this.initial ? INITIAL_RTCP_MIN_TIME : RTCP_MIN_TIME;
		
		// 3 - calculate Td
		double td = Math.max(tMin, n * c);
		
		// 4 - calculate interval T
		double min = td * 0.5;
		double max = td * 1.5;
		double t = min + (max - min) * this.random.nextDouble();
		
		// 5 - divide T by e-3/2
		return t / COMPENSATION;
	}
	
	/**
	 * This function is responsible for deciding whether to send an RTCP report
	 * or BYE packet now, or to reschedule transmission.
	 * 
	 * It is also responsible for updating the pmembers, initial, tp, and
	 * avg_rtcp_size state variables. This function should be called upon
	 * expiration of the event timer used by Schedule().
	 * 
	 * @param task The scheduled task that expired
	 */
	private void onExpire(ScheduledTask task) {
		RtcpPacket packet = task.getPacket();
		double t = rtcpInterval();
		double tn = this.tp + t;
		
		/*
		 * In the case of a BYE, we use "timer reconsideration" to reschedule
		 * the transmission of the BYE if necessary
		 */
		if(packet.isBye()) {
			if(tn <= this.tc) {
				// TODO send BYE packet
				return;
			} else {
				// Schedule(tn, e);
			}
		} else {
			
		}
		
		
	}
	
	public boolean canHandle(byte[] packet) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canHandle(byte[] packet, int dataLength, int offset) {
		// TODO Auto-generated method stub
		return false;
	}

	public byte[] handle(byte[] packet) throws PacketHandlerException {
		return handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws PacketHandlerException {
		if (!canHandle(packet, dataLength, offset)) {
			logger.warn("Cannot handle incoming packet");
			throw new PacketHandlerException("Cannot handle incoming packet");
		}
		
		// Decode the RTCP compound packet
		byte[] trimmedPacket = new byte[dataLength - offset];
		System.arraycopy(packet, offset, trimmedPacket, 0, trimmedPacket.length);
		this.rtcpPacket.decode(trimmedPacket, 0);

		/*
		 * All RTCP packets MUST be sent in a compound packet of at least
		 * two individual packets. The first RTCP packet in the compound packet
		 * MUST always be a report packet to facilitate header validation
		 */
		RtcpReport report = this.rtcpPacket.getInitialReport();
		long ssrc = report.getSsrc();
		
		/*
		 * What we do depends on whether we have left the group, and are waiting
		 * to send a BYE or an RTCP report.
		 */
		switch (this.rtcpPacket.getPacketType()) {
		case RTCP_REPORT:
			
			/*
			 * 6.3.3 - Receiving an RTP or Non-BYE RTCP Packet
			 * 
			 * When an RTP or (non-bye) RTCP packet is received from a participant whose
			 * SSRC is not in the member table, the SSRC is added to the table,
			 * and the value for members is updated once the participant has
			 * been validated.
			 */
			if (!isMember(ssrc) && RtcpPacketType.RTCP_REPORT.equals(this.scheduledTask.getType())) {
				addMember(ssrc);
			}

			break;
		case RTCP_BYE:
			
			/*
			 * 6.3.4 - Receiving an RTCP BYE Packet
			 * 
			 */
			switch (this.scheduledTask.getType()) {
			case RTCP_REPORT:
				
				/*
				 * If the received packet is an RTCP BYE packet, the SSRC is
				 * checked against the member table. If present, the entry is
				 * removed from the table, and the value for members is updated.
				 */
				if(isMember(ssrc)) {
					removeMember(ssrc);
				}
				
				/*
				 * The SSRC is then checked against the sender table. If present,
				 * the entry is removed from the table, and the value for senders is
				 * updated.
				 */
				if(this.statistics.isSender(ssrc)) {
					this.statistics.removeSender(ssrc);
				}

				/*
				 * To make the transmission rate of RTCP packets more adaptive to
				 * changes in group membership, the following "reverse
				 * reconsideration" algorithm SHOULD be executed when a BYE packet
				 * is received that reduces members to a value less than pmembers
				 */
				if (this.members < this.pmembers) {
					this.tn = this.tc + (this.members / this.pmembers) * (this.tn - this.tc);
					this.tp = this.tc - (this.members / this.pmembers) * (this.tc - this.tp);

					// Reschedule the next report for time tn
					reschedule(scheduledTask, this.tn);
					this.pmembers = this.members;
				}
				break;
				
			case RTCP_BYE:
				
				this.members++;
				break;
				
			default:
				break;
			}
			break;
		default:
			this.logger.warn("Unkown RTCP packet type. Dropping packet.");
			break;
		}
		
		/*
		 * For each compound RTCP packet received, the value of
		 * avg_rtcp_size is updated.
		 */
		this.avgRtcpSize = (1 / 16) * dataLength + (15 / 16) * this.avgRtcpSize;
		
		
		return null;
	}
	
	private void handleRtcpBye(RtcpBye packet, long ssrc) {
		/*
		 * 6.3.4 - Receiving an RTCP BYE Packet
		 * 
		 * [...] if the received packet is an RTCP BYE packet, the SSRC is
		 * checked against the member table. If present, the entry is
		 * removed from the table, and the value for members is updated.
		 */
		if(isMember(ssrc)) {
			removeMember(ssrc);
		}
		
		/*
		 * The SSRC is then checked against the sender table. If present,
		 * the entry is removed from the table, and the value for senders is
		 * updated.
		 */
		if(this.statistics.isSender(ssrc)) {
			this.statistics.removeSender(ssrc);
		}
		
		/*
		 * To make the transmission rate of RTCP packets more adaptive to
		 * changes in group membership, the following "reverse
		 * reconsideration" algorithm SHOULD be executed when a BYE packet
		 * is received that reduces members to a value less than pmembers
		 */
		double oldTn = this.tn;
		this.tn = this.tc + (this.members / this.pmembers) * (this.tn - this.tc);
		this.tp = this.tc - (this.members / this.pmembers) * (this.tc - this.tp);
		this.pmembers = this.members;
		
		// The next RTCP packet is rescheduled for transmission at time tn, which is now earlier
		reschedule(oldTn, this.tn);
	}
	
	/**
	 * 
	 * @author Henrique Rosa (henrique.rosa@telestax.com)
	 * 
	 */
	private class ScheduledTask extends TimerTask {
		
		private long timestamp;
		private final RtcpPacketType type;
		private final RtcpPacket packet;
		
		public ScheduledTask(long timestamp, RtcpPacket packet, RtcpPacketType type) {
			this.timestamp = timestamp;
			this.packet = packet;
			this.type = type;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
		
		public RtcpPacket getPacket() {
			return packet;
		}
		
		public RtcpPacketType getType() {
			return type;
		}

		@Override
		public void run() {
			// remove this task from the scheduled packets map
			synchronized (scheduledTask) {
				scheduledTask.remove(this.timestamp);
			}
			
			// send report if channel is open
			if (channel != null && channel.isOpen()) {
				initial = false;
				
			}
			
			/*
			 * 6.3.6 - Expiration of Transmission Timer
			 * 
			 * The transmission interval T is computed, including the
			 * randomization factor.
			 * 
			 * If tp + T is less than or equal to tc, an RTCP packet is
			 * transmitted. tp is set to tc, then another value for T is
			 * calculated as in the previous step and tn is set to tc + T. The
			 * transmission timer is set to expire again at time tn. If tp + T
			 * is greater than tc, tn is set to tp + T. No RTCP packet is
			 * transmitted. The transmission timer is set to expire at time tn.
			 */
			double t = rtcpInterval();
			
			if(tp + t <= tc) {
				// TODO transmit rtcp packet
				tp = tc;
				t = rtcpInterval();
				tn = tc + t;
			} else {
				
			}

		}

	}

}
