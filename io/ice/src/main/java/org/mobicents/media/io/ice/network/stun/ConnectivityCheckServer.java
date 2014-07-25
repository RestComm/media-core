package org.mobicents.media.io.ice.network.stun;

import java.nio.channels.Selector;

import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.io.ice.network.nio.NioServer;

public class ConnectivityCheckServer extends NioServer {

	private final IceAgent agent;
	private final StunHandler stunStack;
	private final StunListener stunListener;

	public ConnectivityCheckServer(IceAgent agent, Selector selector) {
		super(selector);
		this.agent = agent;
		this.stunStack = new StunHandler(this.agent);
		this.addProtocolHandler(stunStack);
		this.stunListener = new StunListenerImpl(this.agent);
		this.stunStack.addListener(this.stunListener);
	}

	protected class StunListenerImpl implements StunListener {

		private final IceAgent agent;

		public StunListenerImpl(IceAgent agent) {
			this.agent = agent;
		}

		public void onBinding(BindingSuccessEvent event) {
			// Tell the ICE agent to select a candidate for the correct media
			// stream and component
			this.agent.selectCandidatePair(event.getChannel());
		}
	}

}
