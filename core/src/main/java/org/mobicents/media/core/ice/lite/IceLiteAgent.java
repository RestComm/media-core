package org.mobicents.media.core.ice.lite;

import org.mobicents.media.core.ice.IceAgent;
import org.mobicents.media.core.ice.network.stun.ConnectivityCheckServer;

public class IceLiteAgent extends IceAgent {

	public IceLiteAgent() {
		super();
	}

	@Override
	public boolean isLite() {
		return true;
	}

	@Override
	public boolean isControlling() {
		return false;
	}

	@Override
	public void start() {
		if (this.running) {
			throw new IllegalStateException("The ICE agent is already started.");
		}
		
		// Candidates must be gathered and a selector available
		if (this.selector == null) {
			throw new IllegalStateException(
					"Cannot start agent without gathering candidates first.");
		}
		// Initialize connectivity server if necessary
		if (this.connectivityCheckServer == null) {
			this.connectivityCheckServer = new ConnectivityCheckServer(this, this.selector);
		}
		// Run connectivity check server
		this.connectivityCheckServer.start();
		this.running = true;
	}

	@Override
	public void stop() {
		if (this.running) {
			// Stop the connectivity check server
			this.connectivityCheckServer.stop();
		}
	}
}
