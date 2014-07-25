package org.mobicents.media.io.ice.network.stun;

import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;

/**
 * Event that should be thrown when a STUN handler replies successfully to a
 * binding request.
 * 
 * @author Henrique Rosa
 * 
 */
public class BindingSuccessEvent {

	private final StunHandler source;
	private final SelectionKey key;

	public BindingSuccessEvent(StunHandler source, SelectionKey key) {
		super();
		this.source = source;
		this.key = key;
	}

	public StunHandler getSource() {
		return source;
	}

	public SelectionKey getKey() {
		return key;
	}
	
	public DatagramChannel getChannel() {
		return (DatagramChannel) this.key.channel();
	}

}
