package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.io.network.handler.ProtocolHandler;
import org.mobicents.media.server.io.network.handler.ProtocolHandlerException;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.Scheduler;

/**
 * Handles incoming RTP packets.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpHandler implements ProtocolHandler {
	
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
	

	public byte[] handle(byte[] packet) throws ProtocolHandlerException {
		return this.handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws ProtocolHandlerException {
		// Do not handle data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.srtp && !this.dtlsHandler.isHandshakeComplete()) {
			return null;
		}
		
		// Wrap the incoming packet into a buffer
		// XXX should use direct buffer????
		//ByteBuffer buffer = ByteBuffer.allocateDirect(dataLength);
		ByteBuffer buffer = ByteBuffer.wrap(packet, offset, dataLength);

		// Convert raw data into an RTP Packet representation
		RtpPacket rtpPacket = new RtpPacket(buffer);
		
		// Decode packet if this is a WebRTC call
		if(this.srtp) {
			rtpPacket = this.dtlsHandler.decode(rtpPacket);
		}

		if(rtpPacket == null) {
			// Handler could not decode the packet, so drop it
			return null;
		}
		
		// Restart jitter buffer for first received packet
		if(this.statistics.getReceived() == 0) {
			this.jitterBuffer.restart();
		}
		
		this.statistics.setLastPacketReceived(clock.getTime());
		
		// RTP v0 packets are used in some applications. Discarded since we do not handle them.
		if (rtpPacket.getVersion() != 0 && (receivable || loopable)) {
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
			}
		}
		return null;
	}
}
