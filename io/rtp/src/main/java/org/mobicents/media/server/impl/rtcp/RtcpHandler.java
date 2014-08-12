package org.mobicents.media.server.impl.rtcp;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	public static final double BW_FRACTION = 0.05;

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
	public static final int MIN_WAIT_INTERVAL = 5000;

	/**
	 * At application startup, a delay SHOULD be imposed before the first
	 * compound RTCP packet is sent to allow time for RTCP packets to be
	 * received from other participants so the report interval will converge to
	 * the correct value more quickly.
	 * 
	 * This delay MAY be set to half the minimum interval to allow quicker
	 * notification that the new participant is present.
	 */
	public static final int INITIAL_DELAY = MIN_WAIT_INTERVAL / 2;

	/**
	 * Stores statistics regarding the RTP session
	 */
	private final RtpStatistics statistics;
	
	/**
	 * Keeps track of scheduled packets to be transmitted.
	 * This is necessary to reschedule packets when the transmission rate is updated.
	 * 
	 * key - Timestamp of the scheduled transmission<br>
	 * value - The scheduled task
	 */
	private final Map<Long, ScheduledTask> scheduledPackets;

	/**
	 * the last time an RTCP packet was transmitted
	 */
	private long tp;

	/**
	 * the current time
	 */
	private long tc;

	/**
	 * the next scheduled transmission time of an RTCP packet
	 */
	private long tn;

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
	private int avgRtcpSize;

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

		// rtcp stuff
		this.statistics = statistics;
		this.scheduledPackets = new HashMap<Long, RtcpHandler.ScheduledTask>();
		this.tp = 0;
		this.tc = 0;
		this.pmembers = 1;
		this.members = 1;
		this.membersList = new ArrayList<Long>();
		this.weSent = false;
		this.rtcpBw = RtpChannel.DEFAULT_BW * BW_FRACTION;
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
		this.tn = System.currentTimeMillis() + INITIAL_DELAY;
		schedule(this.tn);
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
	
	public boolean isMemberRegistered(long ssrc) {
		return isRegistered(ssrc, this.membersList);
	}
	
	private void registerMember(long ssrc) {
		synchronized (this.membersList) {
			this.membersList.add(Long.valueOf(ssrc));
			this.members++;
		}
	}
	
	private void deregisterMember(long ssrc) {
		synchronized (this.membersList) {
			if (this.membersList.remove(Long.valueOf(ssrc))) {
				this.members--;
			}
		}
	}

	public void schedule(long timestamp) {
		if (weSent) {
			// TODO schedule SR
		} else {
			// TODO schedule RR
		}
	}
	
	public void reschedule(long from, long to) {
		ScheduledTask scheduled;
		synchronized (this.scheduledPackets) {
			scheduled = this.scheduledPackets.remove(Long.valueOf(from));
		}
		if(scheduled != null) {
			scheduled.cancel();
			schedule(to);
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
		
		// Decode the RTCP packet
		byte[] trimmedPacket = new byte[dataLength - offset];
		System.arraycopy(packet, offset, trimmedPacket, 0, trimmedPacket.length);
		this.rtcpPacket.decode(trimmedPacket, 0);


		long ssrc;
		if(this.rtcpPacket.isSender()) {
			RtcpSenderReport report = this.rtcpPacket.getRtcpSenderReport();
			ssrc = report.getSsrc();
		} else {
			RtcpReceptionReport report = this.rtcpPacket.getRtcpReceptionReport();
			ssrc = report.getSsrc();
		}

		if (this.rtcpPacket.containsBye()) {
			/*
			 * 6.3.4 - Receiving an RTCP BYE Packet
			 * 
			 * [...] if the received packet is an RTCP BYE packet, the SSRC is
			 * checked against the member table. If present, the entry is
			 * removed from the table, and the value for members is updated.
			 */
			if(isMemberRegistered(ssrc)) {
				deregisterMember(ssrc);
			}
			
			/*
			 * The SSRC is then checked against the sender table. If present,
			 * the entry is removed from the table, and the value for senders is
			 * updated.
			 */
			if(this.statistics.isSenderRegistered(ssrc)) {
				this.statistics.deregisterSender(ssrc);
			}
			
			/*
			 * To make the transmission rate of RTCP packets more adaptive to
			 * changes in group membership, the following "reverse
			 * reconsideration" algorithm SHOULD be executed when a BYE packet
			 * is received that reduces members to a value less than pmembers
			 */
			long oldTn = this.tn;
			this.tn = this.tc + (this.members / this.pmembers) * (this.tn - this.tc);
			this.tp = this.tc - (this.members / this.pmembers) * (this.tc - this.tp);
			this.pmembers = this.members;
			
			// The next RTCP packet is rescheduled for transmission at time tn, which is now earlier
			reschedule(oldTn, this.tn);
			
		} else {
			/*
			 * 6.3.3 - Receiving an RTP or Non-BYE RTCP Packet
			 * 
			 * When an RTP or (non-bye) RTCP packet is received from a participant whose
			 * SSRC is not in the member table, the SSRC is added to the table,
			 * and the value for members is updated once the participant has
			 * been validated.
			 */
			if (!isMemberRegistered(ssrc)) {
				registerMember(ssrc);
			}

			/*
			 * For each compound RTCP packet received, the value of
			 * avg_rtcp_size is updated:
			 * 
			 * avg_rtcp_size = (1/16) * packet_size + (15/16) * avg_rtcp_size
			 */
			this.avgRtcpSize = (1 / 16) * dataLength + (15 / 16) * this.avgRtcpSize;
		}
		
		
		
		return null;
	}
	
	/**
	 * 
	 * @author Henrique Rosa (henrique.rosa@telestax.com)
	 * 
	 */
	private class ScheduledTask extends TimerTask {

		@Override
		public void run() {
			if (channel != null && channel.isOpen()) {
				initial = false;
			}

		}

	}

}
