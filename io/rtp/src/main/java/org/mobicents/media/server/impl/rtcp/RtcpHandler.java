package org.mobicents.media.server.impl.rtcp;

import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.mobicents.media.server.impl.rtp.RtpChannel;
import org.mobicents.media.server.impl.rtp.RtpStatistics;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandler {

	/*
	 * Core stuff
	 */
	private final Scheduler scheduler;
	private final Timer timer;
	private DatagramChannel channel;
	private ByteBuffer byteBuffer;

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

	private final RtpStatistics statistics;

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
	private final List<Long> sessionMembers;

	/**
	 * the most current estimate for the number of senders in the session
	 */
	private int senders;

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

		// rtcp stuff
		this.statistics = statistics;
		this.tp = 0;
		this.tc = 0;
		this.senders = 0;
		this.pmembers = 1;
		this.members = 1;
		this.sessionMembers = new ArrayList<Long>();
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
		this.sessionMembers.add(this.statistics.getSsrc());
		// TODO compute timestamp using algorithm explained in
		// http://tools.ietf.org/html/rfc3550#appendix-A.7
		this.tn = System.currentTimeMillis() + INITIAL_DELAY;
		schedule(this.tn);
	}
	
	public void schedule(long timestamp) {
		if(weSent) {
			// TODO schedule SR 
		} else {
			// TODO schedule RR
		}
	}

	/**
	 * 
	 * @author Henrique Rosa (henrique.rosa@telestax.com)
	 * 
	 */
	private class ScheduledTask extends TimerTask {

		@Override
		public void run() {
			if(channel != null && channel.isOpen()) {

			}

		}

	}

}
