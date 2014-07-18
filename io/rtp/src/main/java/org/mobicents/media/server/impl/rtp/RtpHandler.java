package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.io.network.handler.ProtocolHandler;
import org.mobicents.media.server.io.network.handler.ProtocolHandlerException;
import org.mobicents.media.server.scheduler.Clock;

/**
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtpHandler implements ProtocolHandler {
	
	private RTPFormats formats;
	private final Clock clock;
	private long lastReceivedPacket;
	
	private JitterBuffer jitterBuffer;
	private DtmfInput dtmfInput;
	
	private boolean shouldLoop;
	private boolean shouldReceive;
	
	private volatile int rxCount;
	private volatile int txCount;
	
	public RtpHandler(final Clock clock, final RTPFormats formats) {
		this.formats = formats;
		this.clock = clock;
		this.lastReceivedPacket = 0;
		this.shouldLoop = false;
		this.shouldReceive = false;
		this.rxCount = 0;
		this.txCount = 0;
	}
	
	public long getLastReceivedPacket() {
		return this.lastReceivedPacket;
	}
	
	public int getRxCount() {
		return this.rxCount;
	}
	
	public int getTxCount() {
		return this.txCount;
	}
	
	public boolean canHandle(byte[] packet) {
		// TODO Auto-generated method stub
		return false;
	}

	public byte[] handle(byte[] packet) throws ProtocolHandlerException {
		return this.handle(packet, packet.length, 0);
	}

	public byte[] handle(byte[] packet, int dataLength, int offset) throws ProtocolHandlerException {
		// Convert raw data into an RTP Packet representation
		RtpPacket rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
		ByteBuffer buffer = rtpPacket.getBuffer();
		buffer.put(packet, offset, dataLength);
		
		// TODO decode packet if it is SRTP
		
		this.lastReceivedPacket = clock.getTime();
		
		if (rtpPacket.getVersion() != 0 && (shouldReceive || shouldLoop)) {
			// RTP v0 packets is used in some application.
			// Discarding since we do not handle them
			// Queue packet into the receiver jitter buffer
			if (rtpPacket.getBuffer().limit() > 0) {
				if (shouldLoop) {
					// Increment counters
					this.rxCount++;
					this.txCount++;
					// Return same packet (looping) so it can be transmitted
					return packet;
				} else {
					RTPFormat format = formats.find(rtpPacket.getPayloadType());
					if (format != null && format.getFormat().matches(RtpChannel.DTMF_FORMAT)) {
						dtmfInput.write(rtpPacket);
					} else {
						jitterBuffer.write(rtpPacket, format);
					}
					this.rxCount++;
				}
			}
		}
		return null;
	}
}
