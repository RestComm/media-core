package org.mobicents.media.io.ice.network.stun;

import java.nio.channels.Selector;

import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.io.ice.network.nio.NioServer;

public class ConnectivityCheckServer extends NioServer {

	private final IceAgent agent;
	private final StunHandler stunHandler;
	private final StunListener stunListener;

	public ConnectivityCheckServer(IceAgent agent, Selector selector) {
		super(selector);
		this.agent = agent;
		this.stunHandler = new StunHandler(this.agent);
		this.stunListener = new StunListenerImpl(this.agent);
		this.stunHandler.addListener(this.stunListener);

		// Elected protocol handler on NIO server
		this.protocolHandler = this.stunHandler;
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
