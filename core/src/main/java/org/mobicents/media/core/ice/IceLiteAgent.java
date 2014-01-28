package org.mobicents.media.core.ice;

import org.ice4j.ice.NominationStrategy;

public class IceLiteAgent extends IceAgent {
	
	private static final boolean LITE = true;
	private static final boolean TRICKLE = false;

	public IceLiteAgent() {
		super();
		this.agent.setTrickling(TRICKLE);
		this.agent.setControlling(!LITE);
		this.agent
				.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);
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
