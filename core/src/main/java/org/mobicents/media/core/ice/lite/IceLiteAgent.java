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
		// Candidates must be gathered and a selector available
		if (this.selector == null) {
			throw new IllegalStateException(
					"Cannot start agent without gathering candidates first.");
		}
		// Initialize connectivity server if necessary
		if (this.connectivityCheckServer == null) {
			this.connectivityCheckServer = new ConnectivityCheckServer(this,
					this.selector);
		}
		// Run connectivity check server
		this.connectivityCheckServer.start();
	}
}
