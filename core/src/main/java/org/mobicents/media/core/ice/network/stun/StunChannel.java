package org.mobicents.media.core.ice.network.stun;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.mobicents.media.server.impl.rtp.ChannelsManager;

/**
 * UDP channel that handles incoming/outgoing STUN traffic.
 * 
 * @author Henrique Rosa
 * 
 */
public class StunChannel {

	private DatagramChannel udpChannel;
	private StunHandler stunHandler;

	public StunChannel(ChannelsManager channelsManager) {
	}
	
	private void receivePacket(StunPacket packet) throws IOException {
//		ByteBuffer buffer = packet.getBuffer();
//		buffer.clear();
//		this.udpChannel.read(buffer);
//		buffer.flip();
	}

	private void sendPacket(StunPacket packet) throws IOException {
//		ByteBuffer buffer = packet.getBuffer();
//		buffer.rewind();
//		this.udpChannel.send(buffer, udpChannel.getRemoteAddress());
	}

}
