package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpPacketType;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * Handles incoming RTP packets.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpHandler implements PacketHandler {
	
	private static final Logger logger = Logger.getLogger(RtpHandler.class);
	
	private RTPFormats rtpFormats;
	private final Clock clock;
	private final RtpClock rtpClock;
	private final RtpClock oobClock;
	
	private JitterBuffer jitterBuffer;
	private int jitterBufferSize;
	private final RTPInput rtpInput;
	private final DtmfInput dtmfInput;
	
	private boolean loopable;
	private boolean receivable;
	
	private final RtpStatistics statistics;
	private final RtpPacket rtpPacket;
	
	// SRTP
	private boolean srtp;
	private DtlsHandler dtlsHandler;
	
	public RtpHandler(final Scheduler scheduler, final int jitterBufferSize, final RtpStatistics statistics) {
		this.clock = scheduler.getClock();
		this.rtpClock = new RtpClock(this.clock);
		this.oobClock = new RtpClock(this.clock);
		
		this.jitterBufferSize = jitterBufferSize;
		this.jitterBuffer = new JitterBuffer(this.rtpClock, this.jitterBufferSize);
		
		this.rtpInput = new RTPInput(scheduler, jitterBuffer);
		this.jitterBuffer.setListener(this.rtpInput);
		this.dtmfInput = new DtmfInput(scheduler, oobClock);
		
		this.rtpFormats = new RTPFormats();
		this.statistics = statistics;
		this.rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
		this.receivable = false;
		this.loopable = false;
		
		this.srtp = false;
	}
	
	public RTPInput getRtpInput() {
		return rtpInput;
	}
	
	public DtmfInput getDtmfInput() {
		return dtmfInput;
	}
	
	public boolean isLoopable() {
		return loopable;
	}
	
	public void setLoopable(boolean loopable) {
		this.loopable = loopable;
	}
	
	public boolean isReceivable() {
		return receivable;
	}
	
	public void setReceivable(boolean receivable) {
		this.receivable = receivable;
	}
	
	public void useJitterBuffer(boolean useBuffer) {
		this.jitterBuffer.setBufferInUse(useBuffer);
	}
	
	/**
	 * Modifies the map between format and RTP payload number
	 * 
	 * @param rtpFormats
	 *            the format map
	 */
	public void setFormatMap(final RTPFormats rtpFormats) {
		this.rtpFormats = rtpFormats;
		this.jitterBuffer.setFormats(rtpFormats);
	}
	
	public void enableSrtp(final DtlsHandler handler) {
		this.srtp = true;
		this.dtlsHandler = handler;
	}
	
	public void activate() {
		this.rtpInput.activate();
		this.dtmfInput.activate();
	}
	
	public void deactivate() {
		this.rtpInput.deactivate();
		this.dtmfInput.deactivate();
	}
	
	public void reset() {
		this.deactivate();
		this.dtmfInput.reset();
	}
	
	/*
	 * The RTP header has the following format:
	 *
     * 0                   1                   2                   3
     * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                           timestamp                           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |           synchronization source (SSRC) identifier            |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |            contributing source (CSRC) identifiers             |
     * |                             ....                              |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * 
     * The first twelve octets are present in every RTP packet, while the
     * list of CSRC identifiers is present only when inserted by a mixer.
     * 
     * The version defined by RFC3550 specification is two.
	 */
	
	public boolean canHandle(byte[] packet) {
		return canHandle(packet, packet.length, 0);
	}
	
	public boolean canHandle(byte[] packet, int dataLength, int offset) {
		// Packet must be equal or greater than an RTP Packet Header
		if(dataLength >= RtpPacket.FIXED_HEADER_SIZE) {
			// The most significant 2 bits of every RTP message correspond to the version.
			// Currently supported version is 2 according to RFC3550
			byte b0 = packet[offset];
			int version = (b0 & 0xC0) >> 6;
			return version == RtpPacket.VERSION;
		}
		return false;
	}
	

	public byte[] handle(byte[] packet) throws PacketHandlerException {
		return this.handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws PacketHandlerException {
		// Do not handle data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.srtp && !this.dtlsHandler.isHandshakeComplete()) {
			return null;
		}
		
		// Transform incoming data into an RTP Packet
		ByteBuffer buffer = this.rtpPacket.getBuffer();
		buffer.clear();
		buffer.put(packet, offset, dataLength);
		buffer.flip();
		
		// Decode packet if this is a WebRTC call
		if(this.srtp) {
			if(!this.dtlsHandler.decode(rtpPacket)) {
				logger.warn("SRTP packet is not valid!");
				return null;
			}
		}
		
		// Restart jitter buffer for first received packet
		if(this.statistics.getReceived() == 0) {
			logger.info("Restarting jitter buffer");
			this.jitterBuffer.restart();
		}
		
		this.statistics.setRtpReceivedOn(clock.getTime());
		
		// RTP v0 packets are used in some applications. Discarded since we do not handle them.
		if (rtpPacket.getVersion() != 0 && (receivable || loopable)) {
			/*
			 * When an RTP packet is received from a participant whose SSRC is
			 * not in the sender table, the SSRC is added to the table, and the
			 * value for senders is updated.
			 */
			long ssrc = rtpPacket.getSyncSource();
			
			// Note that there is no point in registering new members if RTCP handler has scheduled a BYE
			if(RtcpPacketType.RTCP_REPORT.equals(this.statistics.getScheduledPacketType())) {
				if (!this.statistics.isSender(ssrc)) {
					this.statistics.addSender(ssrc);
				}

				if (!this.statistics.isMember(ssrc)) {
					this.statistics.addMember(ssrc);
				}
			}
			
			// Queue packet into the jitter buffer
			if (rtpPacket.getBuffer().limit() > 0) {
				if (loopable) {
					// Increment counters
					this.statistics.incrementReceived();
					this.statistics.incrementTransmitted();
					// Return same packet (looping) so it can be transmitted
					return packet;
				} else {
					RTPFormat format = rtpFormats.find(rtpPacket.getPayloadType());
					if (format != null && format.getFormat().matches(RtpChannel.DTMF_FORMAT)) {
						dtmfInput.write(rtpPacket);
					} else {
						jitterBuffer.write(rtpPacket, format);
					}
					this.statistics.incrementReceived();
				}
			} else {
				logger.warn("Skipping packet because limit of the packets buffer is zero");
			}
		}
		return null;
	}
}
