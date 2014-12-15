package org.mobicents.media.io.ice.network.stun;

import java.net.InetSocketAddress;

/**
 * Event that should be thrown when a STUN handler replies successfully to a
 * binding request.
 * 
 * @author Henrique Rosa
 * 
 */
public class BindingSuccessEvent {

	private final StunHandler source;
	private final InetSocketAddress localPeer;
	private final InetSocketAddress remotePeer;

	public BindingSuccessEvent(StunHandler source, InetSocketAddress localPeer, InetSocketAddress remotePeer) {
		super();
		this.source = source;
		this.localPeer = localPeer;
		this.remotePeer = remotePeer;
	}

	public StunHandler getSource() {
		return source;
	}

	public InetSocketAddress getLocalPeer() {
		return localPeer;
	}
	
	public InetSocketAddress getRemotePeer() {
		return remotePeer;
	}

}
