package org.mobicents.media.core.ice;

import org.mobicents.media.core.ice.lite.LiteFoundationRegistry;

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
	protected FoundationsRegistry whichFoundationsRegistry() {
		if (this.foundationsRegistry == null) {
			return new LiteFoundationRegistry();
		}
		return this.foundationsRegistry;
	}
}
