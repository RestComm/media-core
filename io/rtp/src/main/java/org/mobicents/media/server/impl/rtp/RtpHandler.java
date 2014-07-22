package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.mobicents.media.server.impl.rtp.rfc2833.DtmfInput;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
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
	
	public RtpHandler(final Scheduler scheduler, final int jitterBufferSize, final RtpStatistics statistics) {
		this.clock = scheduler.getClock();
		this.rtpClock = new RtpClock(this.clock);
		this.oobClock = new RtpClock(this.clock);
		
		this.jitterBufferSize = jitterBufferSize;
		this.jitterBuffer = new JitterBuffer(this.rtpClock, this.jitterBufferSize);
		this.jitterBuffer.setListener(this.rtpInput);
		
		this.rtpInput = new RTPInput(scheduler, jitterBuffer);
		this.dtmfInput = new DtmfInput(scheduler, oobClock);
		
		this.rtpFormats = new RTPFormats();
		this.statistics = statistics;
		this.receivable = false;
		this.loopable = false;
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
	public void setFormatMap(RTPFormats rtpFormats) {
		this.rtpFormats = rtpFormats;
		this.jitterBuffer.setFormats(rtpFormats);
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
	
	public boolean canHandle(byte[] packet) {
		// TODO Auto-generated method stub
		return true;
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
