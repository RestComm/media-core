package org.mobicents.media.core.ice;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class CandidatePair {

	// TODO add references to local and remote candidate

	private final SelectionKey selectionKey;
	private final int componentId;

	public CandidatePair(int componentId, SelectionKey key) {
		this.componentId = componentId;
		this.selectionKey = key;
	}

	public int getComponentId() {
		return componentId;
	}

	public DatagramChannel getChannel() {
		return (DatagramChannel) selectionKey.channel();
	}

	public int getLocalPort() {
		try {
			return ((InetSocketAddress) getChannel().getLocalAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getLocalAddress() {
		try {
			return ((InetSocketAddress) getChannel().getLocalAddress())
					.getHostName();
		} catch (IOException e) {
			return "";
		}
	}

	public int getRemotePort() {
		try {
			return ((InetSocketAddress) getChannel().getRemoteAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getRemoteAddress() {
		try {
			return ((InetSocketAddress) getChannel().getRemoteAddress())
					.getHostName();
		} catch (IOException e) {
			return "";
		}
	}
	
	public SelectionKey getSelectionKey() {
		return selectionKey;
	}

}
