package org.mobicents.media.server.io.network.channel;

import java.nio.channels.DatagramChannel;

public interface PacketSender {
	
	void send(DatagramChannel channel, byte[] data);

}
