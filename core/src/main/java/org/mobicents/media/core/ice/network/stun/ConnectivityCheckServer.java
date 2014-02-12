package org.mobicents.media.core.ice.network.stun;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.network.nio.NioServer;

public class ConnectivityCheckServer extends NioServer {

	private final IceAgent agent;
	private final StunHandler stunStack;
	private final StunListener stunListener;

	public ConnectivityCheckServer(IceAgent agent, Selector selector) {
		super(selector);
		this.agent = agent;
		this.stunStack = new StunHandler(this.agent);
		this.stunListener = new StunListenerImpl();
		this.addProtocolHandler(stunStack);
	}

	protected class StunListenerImpl implements StunListener {

		public void onSuccessfulResponse(SelectionKey key) {
			agent.selectChannel(key);
		}
	}

}
