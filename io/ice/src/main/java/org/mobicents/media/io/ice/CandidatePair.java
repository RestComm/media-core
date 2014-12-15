package org.mobicents.media.io.ice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class CandidatePair {

	// TODO add references to local and remote candidate

	private final DatagramChannel channel;
	private final int componentId;

	public CandidatePair(int componentId, DatagramChannel channel) {
		this.componentId = componentId;
		this.channel = channel;
	}

	public int getComponentId() {
		return this.componentId;
	}

	public DatagramChannel getChannel() {
		return this.channel;
	}

	public int getLocalPort() {
		try {
			return ((InetSocketAddress) this.channel.getLocalAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getLocalAddress() {
		try {
			return ((InetSocketAddress) this.channel.getLocalAddress()).getHostName();
		} catch (IOException e) {
			return "";
		}
	}

	public int getRemotePort() {
		try {
			return ((InetSocketAddress) this.channel.getRemoteAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getRemoteAddress() {
		try {
			return ((InetSocketAddress) this.channel.getRemoteAddress()).getHostName();
		} catch (IOException e) {
			return "";
		}
	}

}
