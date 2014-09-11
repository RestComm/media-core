package org.mobicents.media.server.impl.rtcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandler implements PacketHandler {

	private static final Logger logger = Logger.getLogger(RtcpHandler.class);
	
	/** Time (in ms) between SSRC Task executions */
	private static final long SSRC_TASK_DELAY = 7000;

	/* Core elements */
	private DatagramChannel channel;
	private ByteBuffer byteBuffer;

	/* RTCP elements */
	private final Timer txTimer;
	private final Timer ssrcTimer;
	
	private TxTask scheduledTask;
	private final SsrcTask ssrcTask;
	
	private final RtpStatistics statistics;

	/** The elapsed time (milliseconds) since an RTCP packet was transmitted */
	private long tp;
	/** The time interval (milliseconds) until next scheduled transmission time of an RTCP packet */
	private long tn;

	/** Flag that is true if the application has not yet sent an RTCP packet */
	private boolean initial;
	
	/** Flag that is true once the handler joined an RTP session */
	private boolean joined;
	
	/** Checks whether communication of this channel is secure. WebRTC calls only. */
	private boolean secure;
	
	/** Handles the DTLS handshake and encodes/decodes secured packets. For WebRTC calls only. */
	private DtlsHandler dtlsHandler;
	
	
	public RtcpHandler(final RtpStatistics statistics) {
		// core stuff
		this.byteBuffer = ByteBuffer.allocateDirect(RtpPacket.RTP_PACKET_MAX_SIZE);

		// rtcp stuff
		this.txTimer = new Timer();
		this.ssrcTimer = new Timer();
		this.ssrcTask = new SsrcTask();

		this.statistics = statistics;
		this.scheduledTask = null;
		this.tp = 0;
		this.tn = -1;
		this.initial = true;
		this.joined = false;
		
		// webrtc
		this.secure = false;
		this.dtlsHandler = null;
	}

	/**
	 * Gets the time stamp of a future moment in time.
	 * 
	 * @param delay
	 *            The amount of time in the future, in milliseconds
	 * @return The time stamp of the date matching the delay, in milliseconds
	 */
	private long resolveDelay(long delay) {
		return this.statistics.getCurrentTime() + delay;
	}

	/**
	 * Gets the time interval between the current time and another time stamp.
	 * 
	 * @param timestamp
	 *            The time stamp, in milliseconds, to compare to the current time
	 * @return The interval of time between both time stamps, in milliseconds.
	 */
	private long resolveInterval(long timestamp) {
		return timestamp - this.statistics.getCurrentTime();
	}
	
	public void setChannel(DatagramChannel channel) {
		this.channel = channel;
	}

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
	public void joinRtpSession() {
		if(!this.joined) {
			// Schedule first RTCP packet
			long t = this.statistics.rtcpInterval(this.initial);
			RtcpPacket report = RtcpPacketFactory.buildReport(this.statistics);
			
			this.tn = this.statistics.getCurrentTime() + t;
			schedule(this.tn, report);
			
			// Start SSRC timeout timer
			this.ssrcTimer.scheduleAtFixedRate(this.ssrcTask, SSRC_TASK_DELAY, SSRC_TASK_DELAY);

			this.joined = true;
		}
	}
	
	public void leaveRtpSession() {
		if (this.joined) {
			logger.info("Leaving RTP Session.");
			
			// Stop SSRC checks
			this.ssrcTimer.cancel();
			this.ssrcTimer.purge();
			
			// Create a RTCP BYE packet to be scheduled
			RtcpPacket bye = RtcpPacketFactory.buildBye(this.statistics);

			/*
			 * When the participant decides to leave the system, tp is reset to tc,
			 * the current time, members and pmembers are initialized to 1, initial
			 * is set to 1, we_sent is set to false, senders is set to 0, and
			 * avg_rtcp_size is set to the size of the compound BYE packet.
			 * 
			 * The calculated interval T is computed. The BYE packet is then
			 * scheduled for time tn = tc + T.
			 */
			this.tp = this.statistics.getCurrentTime();
			this.statistics.resetMembers();
			this.initial = true;
			this.statistics.clearSenders();
			this.statistics.setRtcpAvgSize(bye.getSize());

			long t = this.statistics.rtcpInterval(initial);
			this.tn = resolveDelay(t);
			schedule(this.tn, bye);
			
			this.joined = false;
		}
	}

	/**
	 * Schedules an event to occur at a certain time.
	 * 
	 * @param timestamp
	 *            The time (in milliseconds) when the event should be fired
	 * @param packet
	 *            The RTCP packet to be sent when the timer expires
	 */
	private void schedule(long timestamp, RtcpPacket packet) {
		// Create the task and schedule it
		long interval = resolveInterval(timestamp);
		this.scheduledTask = new TxTask(packet);
		this.txTimer.schedule(this.scheduledTask, interval);
		// Let the RTP handler know what is the type of scheduled packet
		this.statistics.setRtcpPacketType(packet.getPacketType());
	}

	/**
	 * Re-schedules a previously scheduled event
	 * 
	 * @param timestamp
	 *            The timestamp (in nanoseconds) of the rescheduled event
	 */
	private void reschedule(TxTask task, long timestamp) {
		task.cancel();
		this.txTimer.schedule(task, TimeUnit.NANOSECONDS.toMillis(timestamp));
	}

	/**
	 * Secures the channel, meaning all traffic is SRTCP.
	 * 
	 * SRTCP handlers will only be available to process traffic after a DTLS
	 * handshake is completed.
	 * 
	 * @param remotePeerFingerprint
	 *            The DTLS fingerprint of the remote peer. Use to setup DTLS
	 *            keying material.
	 */
	public void enableSRTCP(DtlsHandler dtlsHandler) {
		this.dtlsHandler = dtlsHandler;
		this.secure = true;
	}
	
	/**
	 * This function is responsible for deciding whether to send an RTCP report
	 * or BYE packet now, or to reschedule transmission.
	 * 
	 * It is also responsible for updating the pmembers, initial, tp, and
	 * avg_rtcp_size state variables. This function should be called upon
	 * expiration of the event timer used by Schedule().
	 * 
	 * @param task
	 *            The scheduled task whose timer expired
	 * 
	 * @throws IOException
	 *             When a packet cannot be sent over the datagram channel
	 */
	private void onExpire(TxTask task) throws IOException {
		long tc = this.statistics.getCurrentTime();
		switch (task.getType()) {
		case RTCP_REPORT:
			long t = this.statistics.rtcpInterval(this.initial);
			this.tn = this.tp + t;

			if (this.tn <= tc) {
				// Send currently scheduled packet and update statistics
				RtcpPacket packet = task.getPacket();
				sendRtcpPacket(packet);

				this.tp = tc;

				/*
				 * We must redraw the interval. Don't reuse the one computed
				 * above, since its not actually distributed the same, as we are
				 * conditioned on it being small enough to cause a packet to be
				 * sent.
				 */
				t = this.statistics.rtcpInterval(this.initial);
				this.tn = tc + t;
				
				// schedule next packet
				RtcpPacket nextPacket = RtcpPacketFactory.buildReport(this.statistics);
				schedule(this.tn, nextPacket);
				initial = false;
			} else {
				// Schedule next packet
				RtcpPacket nextPacket = RtcpPacketFactory.buildReport(this.statistics);
				schedule(tn, nextPacket);
			}

			this.statistics.confirmMembers();
			break;

		case RTCP_BYE:
			/*
			 * In the case of a BYE, we use "timer reconsideration" to
			 * reschedule the transmission of the BYE if necessary
			 */
			t = this.statistics.rtcpInterval(this.initial);
			this.tn = this.tp + t;

			if (this.tn <= tc) {
				// Send BYE and stop scheduling further packets
				sendRtcpPacket(task.getPacket());
				stop();
				closeChannel();
				return;
			} else {
				// Delay BYE
				RtcpPacket nextPacket = RtcpPacketFactory.buildBye(this.statistics);
				schedule(this.tn, nextPacket);
			}
			break;

		default:
			logger.warn("Unkown scheduled event type!");
			break;
		}
	}

	public boolean canHandle(byte[] packet) {
		return canHandle(packet, packet.length, 0);
	}

	public boolean canHandle(byte[] packet, int dataLength, int offset) {
		// RTP version field must equal 2
		int version = (packet[offset] & 0xC0) >> 6;
		if (version == RtpPacket.VERSION) {
			// The payload type field of the first RTCP packet in a compound
			// packet must be equal to SR or RR.
			int type = packet[offset + 1] & 0x000000FF;
			if (type == RtcpHeader.RTCP_SR || type == RtcpHeader.RTCP_RR) {
				/*
				 * The padding bit (P) should be zero for the first packet of a
				 * compound RTCP packet because padding should only be applied,
				 * if it is needed, to the last packet.
				 */
				int padding = (packet[offset] & 0x20) >> 5;
				if(padding == 0) {
					/*
					 * TODO The length fields of the individual RTCP packets must add
					 * up to the overall length of the compound RTCP packet as
					 * received. This is a fairly strong check.
					 */
					return true;
				}
			}
		}
		return false;
	}

	public byte[] handle(byte[] packet) throws PacketHandlerException {
		return handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws PacketHandlerException {
		if (!canHandle(packet, dataLength, offset)) {
			logger.warn("Cannot handle incoming packet!");
			throw new PacketHandlerException("Cannot handle incoming packet");
		}
		
		// Do NOT handle data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.secure && !this.dtlsHandler.isHandshakeComplete()) {
			return null;
		}
		
		// Decode the RTCP compound packet
		RtcpPacket rtcpPacket = new RtcpPacket();
		if(this.secure) {
			byte[] decoded = this.dtlsHandler.decodeRTCP(packet, offset, dataLength);
			if(decoded == null || decoded.length == 0) {
				logger.warn("Could not decode incoming SRTCP packet. Packet will be dropped.");
				return null;
			}
			rtcpPacket.decode(decoded, 0);
		} else {
			rtcpPacket.decode(packet, offset);
		}
		
		// Trace incoming RTCP report
		logger.info("\nINCOMING "+ rtcpPacket.toString());
		
		// Upgrade RTCP statistics
		this.statistics.onRtcpReceive(rtcpPacket);

		if(RtcpPacketType.RTCP_BYE.equals(rtcpPacket.getPacketType())) {
			if(RtcpPacketType.RTCP_REPORT.equals(this.scheduledTask.getType())) {
				/*
				 * To make the transmission rate of RTCP packets more adaptive
				 * to changes in group membership, the following "reverse
				 * reconsideration" algorithm SHOULD be executed when a BYE
				 * packet is received that reduces members to a value less than
				 * pmembers
				 */
				if (this.statistics.getMembers() < this.statistics.getPmembers()) {
					long tc = this.statistics.getCurrentTime();
					this.tn = tc + (this.statistics.getMembers() / this.statistics.getPmembers()) * (this.tn - tc);
					this.tp = tc - (this.statistics.getMembers() / this.statistics.getPmembers()) * (tc - this.tp);

					// Reschedule the next report for time tn
					reschedule(this.scheduledTask, this.tn);
					this.statistics.confirmMembers();
				}
			}
		}
		// RTCP handler does not send replies
		return null;
	}

	private void sendRtcpPacket(RtcpPacket packet) throws IOException {
		if (this.channel != null && channel.isOpen() && channel.isConnected()) {
			// decode packet
			byte[] data = new byte[RtpPacket.RTP_PACKET_MAX_SIZE];
			int dataLength = packet.encode(data, 0);
			
			// If channel is secure, convert RTCP packet to SRTCP. WebRTC calls only.
			if(this.secure) {
				// Skip RTCP packets until DTLS handshake is complete
				if(!this.dtlsHandler.isHandshakeComplete()) {
					return;
				}
				data = this.dtlsHandler.encodeRTCP(data, 0, dataLength);
				dataLength = data.length;
			}

			// prepare buffer
			this.byteBuffer.clear();
			this.byteBuffer.put(data, 0, dataLength);
			this.byteBuffer.flip();
			
			// trace outgoing RTCP report
			logger.info("\nOUTGOING "+ packet.toString());
			
			// update RTCP statistics
			this.statistics.onRtcpSent(packet);

			// send packet
			this.channel.send(this.byteBuffer, this.channel.getRemoteAddress());
		} else {
			logger.warn("Could not send RTCP packet because channel is closed.");
		}
	}
	
	/**
	 * Stops the scheduled task (if any) and cancels the timers
	 */
	private void stop() {
		this.scheduledTask.cancel();
		this.scheduledTask = null;
		this.txTimer.cancel();
		this.txTimer.purge();
		this.ssrcTimer.cancel();
		this.ssrcTimer.purge();
	}
	
	/**
	 * Disconnects and closes the datagram channel used to send and receive RTCP
	 * traffic.
	 */
	private void closeChannel() {
		if(this.channel != null) {
			if(this.channel.isConnected()) {
				try {
					this.channel.disconnect();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}

			if(this.channel.isOpen()) {
				try {
					this.channel.close();
				} catch (IOException e) {
					logger.warn(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Schedulable task responsible for sending RTCP packets.
	 * 
	 * @author Henrique Rosa (henrique.rosa@telestax.com)
	 * 
	 */
	private class TxTask extends TimerTask {

		private final RtcpPacket packet;

		public TxTask(RtcpPacket packet) {
			this.packet = packet;
		}

		public RtcpPacket getPacket() {
			return this.packet;
		}

		public RtcpPacketType getType() {
			return this.packet.getPacketType();
		}

		@Override
		public void run() {
			try {
				onExpire(this);
			} catch (IOException e) {
				logger.error("An error occurred while executing a scheduled task. Stopping handler.", e);
				stop();
			}
		}

	}

	/**
	 * Schedulable task responsible for checking timeouts of registered SSRC.
	 * 
	 * @author Henrique Rosa
	 * 
	 */
	private class SsrcTask extends TimerTask {

		@Override
		public void run() {
			statistics.isSenderTimeout();
		}

	}

}
