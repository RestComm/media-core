package org.mobicents.media.core.ice.lite;

import org.mobicents.media.core.ice.IceAgent;


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
		this.stunServer.start();
	}
}
