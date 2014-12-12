package org.mobicents.media.server.impl.rtp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpHeader;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.channel.PacketHandler;
import org.mobicents.media.server.io.network.channel.PacketHandlerException;
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
	
	private int pipelinePriority;
	
	private RTPFormats rtpFormats;
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
	private boolean secure;
	private DtlsHandler dtlsHandler;
	
	public RtpHandler(Scheduler scheduler, RtpClock clock, RtpClock oobClock, int jitterBufferSize, RtpStatistics statistics) {
		this.pipelinePriority = 0;
		
		this.rtpClock = clock;
		this.oobClock = oobClock;
		
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
		
		this.secure = false;
	}
	
	public int getPipelinePriority() {
		return pipelinePriority;
	}
	
	public void setPipelinePriority(int pipelinePriority) {
		this.pipelinePriority = pipelinePriority;
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
		this.secure = true;
		this.dtlsHandler = handler;
	}
	
	public void disableSrtp() {
		this.secure = false;
		this.dtlsHandler = null;
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
		
		if(this.secure) {
			disableSrtp();
		}
	}
	
	public boolean canHandle(byte[] packet) {
		return canHandle(packet, packet.length, 0);
	}
	
	public boolean canHandle(byte[] packet, int dataLength, int offset) {
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
		// Packet must be equal or greater than an RTP Packet Header
		if(dataLength >= RtpPacket.FIXED_HEADER_SIZE) {
			// The most significant 2 bits of every RTP message correspond to the version.
			// Currently supported version is 2 according to RFC3550
			byte b0 = packet[offset];
			int version = (b0 & 0xC0) >> 6;
			
			if(RtpPacket.VERSION == version) {
				/*
				 * When RTP and RTCP packets are multiplexed onto a single port,
				 * the RTCP packet type field occupies the same position in the
				 * packet as the combination of the RTP marker (M) bit and the
				 * RTP payload type (PT). This field can be used to distinguish
				 * RTP and RTCP packets when two restrictions are observed:
				 * 
				 * 1) the RTP payload type values used are distinct from the
				 * RTCP packet types used.
				 * 
				 * 2) for each RTP payload type (PT), PT+128 is distinct from
				 * the RTCP packet types used. The first constraint precludes a
				 * direct conflict between RTP payload type and RTCP packet
				 * type; the second constraint precludes a conflict between an
				 * RTP data packet with the marker bit set and an RTCP packet.
				 */
				int type = packet[offset + 1] & 0xff & 0x7f;
				int rtcpType = type + 128;
				
				// RTP payload types 72-76 conflict with the RTCP SR, RR, SDES, BYE,
			    // and APP packets defined in the RTP specification
				switch (rtcpType) {
				case RtcpHeader.RTCP_SR:
				case RtcpHeader.RTCP_RR:
				case RtcpHeader.RTCP_SDES:
				case RtcpHeader.RTCP_BYE:
				case RtcpHeader.RTCP_APP:
					return false;
				default:
					return true;
				}
			}
		}
		return false;
	}
	
	public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		return this.handle(packet, packet.length, 0, localPeer, remotePeer);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		System.out.println("RECEIVED RTP PACKET");
		
		// Do not handle data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.secure && !this.dtlsHandler.isHandshakeComplete()) {
			return null;
		}
		
		if(this.secure) {
			// Decode SRTP packet into RTP. WebRTC calls only.
			byte[] decoded = this.dtlsHandler.decodeRTP(packet, offset, dataLength);
			if(decoded == null || decoded.length == 0) {
				logger.warn("SRTP packet is not valid! Dropping packet.");
				return null;
			} else {
				// Transform incoming data directly into an RTP Packet
				ByteBuffer buffer = this.rtpPacket.getBuffer();
				buffer.clear();
				buffer.put(decoded);
				buffer.flip();
			}
		} else {
			// Transform incoming data directly into an RTP Packet
			ByteBuffer buffer = this.rtpPacket.getBuffer();
			buffer.clear();
			buffer.put(packet, offset, dataLength);
			buffer.flip();
		}
		
		// Restart jitter buffer for first received packet
		if(this.statistics.getRtpPacketsReceived() == 0) {
			logger.info("Restarting jitter buffer");
			this.jitterBuffer.restart();
		}
		
		// For RTP keep-alive purposes
		this.statistics.setLastHeartbeat(this.rtpClock.getWallClock().getTime());
		
		// RTP v0 packets are used in some applications. Discarded since we do not handle them.
		if (rtpPacket.getVersion() != 0 && (receivable || loopable)) {
			// Queue packet into the jitter buffer
			if (rtpPacket.getBuffer().limit() > 0) {
				if (loopable) {
					// Update statistics for RTCP
					this.statistics.onRtpReceive(rtpPacket);
					this.statistics.onRtpSent(rtpPacket);
					// Return same packet (looping) so it can be transmitted
					return packet;
				} else {
					// Update statistics for RTCP
					this.statistics.onRtpReceive(rtpPacket);
					// Write packet
					int payloadType = rtpPacket.getPayloadType();
					RTPFormat format = rtpFormats.find(payloadType);
					if(format != null) {
						if(RtpChannel.DTMF_FORMAT.matches(format.getFormat())) {
							dtmfInput.write(rtpPacket);
						} else {
							jitterBuffer.write(rtpPacket, format);
						}
					} else {
						logger.warn("Dropping packet because payload type (" + payloadType + ") is unknown.");
					}
				}
			} else {
				logger.warn("Skipping packet because limit of the packets buffer is zero");
			}
		}
		return null;
	}
	
	public int compareTo(PacketHandler o) {
		if(o == null) {
			return 1;
		}
		return this.getPipelinePriority() - o.getPipelinePriority();
	}
}
