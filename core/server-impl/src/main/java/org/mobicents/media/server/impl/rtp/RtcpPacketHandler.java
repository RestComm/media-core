package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;
import org.mobicents.media.server.impl.rtcp.RtcpPacket;

/**
 * 
 * @author amit bhayani
 *
 */
public class RtcpPacketHandler extends PacketHandler {
	
	private static final Logger logger = Logger.getLogger(RtcpPacketHandler.class);

	private RtpSocketImpl rtpSocketImpl = null;
	
	public RtcpPacketHandler(RtpSocketImpl rtpSocketImpl) {
		this.rtpSocketImpl = rtpSocketImpl;
	}

	@Override
	public void close() {
		this.rtpSocketImpl.close();
	}

	@Override
	public boolean isClosed() {
		return this.rtpSocketImpl.isClosed();
	}

	@Override
	public void receive(ByteBuffer readBuffer) {
		int len = readBuffer.limit();

		// TODO optimize the RTCPPacket to use the ByteBuffer directly
		byte[] buff = new byte[len];

		readBuffer.get(buff, 0, len);

		RtcpPacket rtcpPacket = new RtcpPacket();

		try {
			int length = rtcpPacket.decode(buff, 0);
			this.rtpSocketImpl.receiveRtcp(rtcpPacket);
		} catch (RuntimeException e) {
			logger.error("exception while handling RtcpPacket ", e);
		}
	}

}
