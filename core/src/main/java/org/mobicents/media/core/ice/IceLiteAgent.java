package org.mobicents.media.core.ice;


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
}
