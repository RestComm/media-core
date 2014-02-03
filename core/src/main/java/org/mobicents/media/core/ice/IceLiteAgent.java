package org.mobicents.media.core.ice;


public class IceLiteAgent extends IceAgent {
	
	private static final boolean LITE = true;
	private static final boolean TRICKLE = false;

	public IceLiteAgent() {
		super();
		this.agent.setTrickling(TRICKLE);
		this.agent.setControlling(!LITE);
	}

	@Override
	public boolean isLite() {
		return LITE;
	}

	@Override
	public boolean isTrickling() {
		return TRICKLE;
	}
}
