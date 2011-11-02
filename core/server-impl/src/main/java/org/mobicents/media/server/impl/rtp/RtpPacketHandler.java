package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;

/**
 * 
 * @author amit bhayani
 *
 */
public class RtpPacketHandler extends PacketHandler {

	private RtpSocketImpl rtpSocketImpl = null;
	
	public RtpPacketHandler(RtpSocketImpl rtpSocketImpl) {
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
		RtpPacket rtpPacket = new RtpPacket(readBuffer);
		this.rtpSocketImpl.receiveRtp(rtpPacket);
	}

}
