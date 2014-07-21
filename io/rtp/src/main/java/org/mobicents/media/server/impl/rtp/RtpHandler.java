package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.io.network.handler.ProtocolHandler;
import org.mobicents.media.server.io.network.handler.ProtocolHandlerException;
import org.mobicents.media.server.scheduler.Clock;

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
	private long lastReceivedPacket;
	
	private JitterBuffer jitterBuffer;
	private DtmfInput dtmfInput;
	
	private boolean shouldLoop;
	private boolean shouldReceive;
	
	private final RtpStatistics statistics;
	
	public RtpHandler(final Clock clock, final RtpStatistics statistics) {
		this.rtpFormats = new RTPFormats();
		this.statistics = statistics;
		this.lastReceivedPacket = 0;
		this.shouldReceive = false;
		this.shouldLoop = false;
		this.clock = clock;
	}
	
	public long getLastReceivedPacket() {
		return this.lastReceivedPacket;
	}
	
	/**
	 * Modifies the map between format and RTP payload number
	 * 
	 * @param rtpFormats
	 *            the format map
	 */
	public void setFormatMap(RTPFormats rtpFormats) {
		this.flush();
		this.rtpFormats = rtpFormats;
		this.jitterBuffer.setFormats(rtpFormats);
	}
	
	private void flush() {
		// TODO cleanup resources
	}
	
	public boolean canHandle(byte[] packet) {
		// TODO Auto-generated method stub
		return false;
	}

	public byte[] handle(byte[] packet) throws ProtocolHandlerException {
		return this.handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws ProtocolHandlerException {
		// XXX Creating an RTP packet every time can be memory consuming because of the enclosing ByteBuffer!!!
		// Convert raw data into an RTP Packet representation
		RtpPacket rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
		ByteBuffer buffer = rtpPacket.getBuffer();
		buffer.put(packet, offset, dataLength);
		
		// TODO decode packet if it is SRTP
		
		this.lastReceivedPacket = clock.getTime();
		
		// RTP v0 packets are used in some applications. Discarded since we do not handle them.
		if (rtpPacket.getVersion() != 0 && (shouldReceive || shouldLoop)) {
			// Queue packet into the jitter buffer
			if (rtpPacket.getBuffer().limit() > 0) {
				if (shouldLoop) {
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
