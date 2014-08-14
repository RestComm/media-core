package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfOutput;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import org.mobicents.media.server.impl.srtp.DtlsHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.memory.Frame;

/**
 * Transmits RTP packets over a channel.
 * 
 * @author Oifa Yulian
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpTransmitter {
	
	private static final Logger LOGGER = Logger.getLogger(RtpTransmitter.class);
	
	// Channel properties
	private DatagramChannel channel;
	private final RtpClock rtpClock;
	private final RtpStatistics statistics;
	private boolean dtmfSupported;
	private final RTPOutput rtpOutput;
	private final DtmfOutput dtmfOutput;

	// Packet representations with internal buffers
	private final RtpPacket rtpPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
	private final RtpPacket oobPacket = new RtpPacket(RtpPacket.RTP_PACKET_MAX_SIZE, true);
	
	// WebRTC
	private DtlsHandler dtlsHandler;
	private boolean srtp;

	// Details of a transmitted packet
	private RTPFormats formats;
	private RTPFormat currentFormat;
	private long timestamp;
	private long dtmfTimestamp;
	
	public RtpTransmitter(final Scheduler scheduler, final RtpStatistics statistics) {
		this.rtpClock = new RtpClock(scheduler.getClock());
		this.statistics = statistics;
		this.dtmfSupported = false;
		this.rtpOutput = new RTPOutput(scheduler, this);
		this.dtmfOutput = new DtmfOutput(scheduler, this);
		this.dtmfTimestamp = -1;
		this.timestamp = -1;
		this.formats = null;
		this.srtp = false;
	}
	
	public void setFormatMap(final RTPFormats rtpFormats) {
		this.dtmfSupported = rtpFormats.contains(AVProfile.telephoneEventsID);
		this.formats = rtpFormats;
	}
	
	public RTPOutput getRtpOutput() {
		return rtpOutput;
	}
	
	public DtmfOutput getDtmfOutput() {
		return dtmfOutput;
	}
	
	public void enableSrtp(final DtlsHandler handler) {
		this.srtp = true;
		this.dtlsHandler = handler;
	}
	
	public void activate() {
		this.rtpOutput.activate();
		this.dtmfOutput.activate();
	}
	
	public void deactivate() {
		this.rtpOutput.deactivate();
		this.dtmfOutput.deactivate();
	}
	
	public void setChannel(final DatagramChannel channel) {
		this.channel = channel;
	}
	
	private boolean isConnected() {
		return this.channel != null && this.channel.isConnected();
	}
	
	private void disconnect() throws IOException {
		if(this.channel != null) {
			this.channel.disconnect();
		}
	}
	
	public void reset() {
		this.rtpOutput.deactivate();
		this.dtmfOutput.deactivate();
		this.dtmfSupported = false;
		this.clear();
	}
	
	public void clear() {
		this.timestamp = -1;
		this.dtmfTimestamp = -1;
		// Reset format in case connection is reused.
		// Otherwise it would point to incorrect codec.
		this.currentFormat = null;
	}
	
	private void send(RtpPacket packet) throws IOException {
		// Do not send data while DTLS handshake is ongoing. WebRTC calls only.
		if(this.srtp && !this.dtlsHandler.isHandshakeComplete()) {
			return;
		}
		
		// Secure RTP packet. WebRTC calls only. 
		// SRTP handler returns null if an error occurs
		if (this.srtp) {
			this.dtlsHandler.encode(packet);
		}
		
		if(packet != null) {
			// Rewind buffer
			ByteBuffer buf = packet.getBuffer();
			buf.rewind();
			
			// send RTP packet to the network
			channel.send(buf, channel.socket().getRemoteSocketAddress());
			statistics.incrementTransmitted();
		}
	}
	
	public void sendDtmf(Frame frame) {
		if (!this.dtmfSupported) {
			frame.recycle();
			return;
		}
		
		// ignore frames with duplicate timestamp
		if (frame.getTimestamp() / 1000000L == dtmfTimestamp) {
			frame.recycle();
			return;
		}

		// convert to milliseconds first
		dtmfTimestamp = frame.getTimestamp() / 1000000L;
		// convert to rtp time units
		dtmfTimestamp = rtpClock.convertToRtpTime(dtmfTimestamp);
		oobPacket.wrap(false, AVProfile.telephoneEventsID, statistics.nextSequenceNumber(), dtmfTimestamp, this.statistics.getSsrc(), frame.getData(), frame.getOffset(), frame.getLength());

		frame.recycle();
		
		try {
			if(isConnected()) {
				send(oobPacket);
			}
		} catch (PortUnreachableException e) {
			try {
				// icmp unreachable received
				// disconnect and wait for new packet
				disconnect();
			} catch (IOException ex) {
				LOGGER.error(ex.getMessage(), ex);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	public void send(Frame frame) {
		// discard frame if format is unknown
		if (frame.getFormat() == null) {
			frame.recycle();
			return;
		}

		// determine current RTP format if it is unknown
		if (currentFormat == null || !currentFormat.getFormat().matches(frame.getFormat())) {
			currentFormat = formats.getRTPFormat(frame.getFormat());
			// discard packet if format is still unknown
			if (currentFormat == null) {
				frame.recycle();
				return;
			}
			// update clock rate
			rtpClock.setClockRate(currentFormat.getClockRate());
		}

		// ignore frames with duplicate timestamp
		if (frame.getTimestamp() / 1000000L == timestamp) {
			frame.recycle();
			return;
		}

		// convert to milliseconds first
		timestamp = frame.getTimestamp() / 1000000L;
		// convert to rtp time units
		timestamp = rtpClock.convertToRtpTime(timestamp);
		rtpPacket.wrap(false, currentFormat.getID(), statistics.nextSequenceNumber(), timestamp, this.statistics.getSsrc(), frame.getData(), frame.getOffset(), frame.getLength());

		frame.recycle();
		try {
			if (isConnected()) {
				send(rtpPacket);
			}
		} catch (PortUnreachableException e) {
			// icmp unreachable received
			// disconnect and wait for new packet
			try {
				disconnect();
			} catch (IOException ex) {
				LOGGER.error(ex.getMessage(), ex);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

}
