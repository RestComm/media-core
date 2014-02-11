package org.mobicents.media.core.ice.network.nio;

import java.nio.channels.DatagramChannel;

/**
 * Represent an event that is scheduled to be fired on a Server.
 * 
 * @author Henrique Rosa
 * 
 */
public class ScheduledEvent {

	private NioServer server;
	private DatagramChannel channel;
	private byte[] data;

	public ScheduledEvent(NioServer server, DatagramChannel channel, byte[] data) {
		super();
		this.server = server;
		this.channel = channel;
		this.data = data;
	}

	public void launch() {
		this.server.send(channel, data);
	}

}
