package org.mobicents.media.io.ice.network.stun;

import org.mobicents.media.io.ice.IceAgent;
import org.mobicents.media.server.io.network.server.NioServer;

/**
 * Non-blocking server to listen to STUN connectivity checks that happen as part
 * of the ICE handshake.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectivityCheckServer extends NioServer {

	private final IceAgent agent;
	private final StunHandler stunHandler;
	private final StunListener stunListener;

	public ConnectivityCheckServer(IceAgent agent) {
		super();
		this.agent = agent;
		this.stunHandler = new StunHandler(this.agent);
		this.stunListener = new StunListenerImpl(this.agent);
		this.stunHandler.addListener(this.stunListener);
		super.addPacketHandler(this.stunHandler);
	}

	protected class StunListenerImpl implements StunListener {

		private final IceAgent agent;

		public StunListenerImpl(IceAgent agent) {
			this.agent = agent;
		}

		public void onBinding(BindingSuccessEvent event) {
			
			// Tell the ICE agent to select a candidate for the correct media
			// stream and component
			this.agent.selectCandidatePair(ConnectivityCheckServer.this.currentChannel);
		}
		
	}

}
