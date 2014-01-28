package org.mobicents.media.core.ice;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.NominationStrategy;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.security.LongTermCredential;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public final class IceFullAgent extends IceAgent {

	private static final boolean LITE = false;
	private static final int STUN_PORT = 3478;
	private static final Transport TRANSPORT_UDP = Transport.UDP;

	public static final String STUN_SERVER_JITSI = "stun.jitsi.net";
	public static final String STUN6_SERVER_JITSI = "stun6.jitsi.net";
	public static final String STUN_SERVER_JITSI_USERNAME = "guest";
	public static final String STUN_SERVER_JITSI_PASSWORD = "anonymouspower!!";

	private final boolean stunSupported;
	private final boolean turnSupported;

	public IceFullAgent() {
		this(false);
	}

	public IceFullAgent(final boolean trickle) {
		this(trickle, false, false);
	}

	public IceFullAgent(final boolean trickle, final boolean useStun,
			final boolean useTurn) {
		super();
		this.stunSupported = useStun;
		this.turnSupported = useTurn;

		this.agent.setControlling(!LITE);
		this.agent.setTrickling(trickle);
		this.agent
				.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);

		if (this.stunSupported || this.turnSupported) {
			LongTermCredential credential = new LongTermCredential(
					STUN_SERVER_JITSI_USERNAME, STUN_SERVER_JITSI_PASSWORD);
			if (this.stunSupported) {
				agent.addCandidateHarvester(createStunHarverster(
						STUN_SERVER_JITSI, credential));
				agent.addCandidateHarvester(createStunHarverster(
						STUN6_SERVER_JITSI, credential));
			}

			if (this.turnSupported) {
				agent.addCandidateHarvester(createTurnHarverster(
						STUN_SERVER_JITSI, credential));
				agent.addCandidateHarvester(createTurnHarverster(
						STUN6_SERVER_JITSI, credential));
			}
		}

		// UPnP: adding an UPnP harvester because they are generally slow which
		// makes it more convenient to test things like trickle.
		agent.addCandidateHarvester(new UPNPHarvester());
	}

	/**
	 * Indicates whether STUN is supported
	 * 
	 * @return
	 */
	public boolean isStunSupported() {
		return stunSupported;
	}

	/**
	 * Indicates whether TURN is supported
	 * 
	 * @return
	 */
	public boolean isTurnSupported() {
		return turnSupported;
	}

	@Override
	public boolean isLite() {
		return LITE;
	}

	@Override
	public boolean isTrickling() {
		return this.agent.isTrickling();
	}

	private StunCandidateHarvester createStunHarverster(String hostname,
			LongTermCredential credential) {
		return new StunCandidateHarvester(new TransportAddress(hostname,
				STUN_PORT, TRANSPORT_UDP));
	}

	private TurnCandidateHarvester createTurnHarverster(String hostname,
			LongTermCredential credential) {
		return new TurnCandidateHarvester(new TransportAddress(hostname,
				STUN_PORT, TRANSPORT_UDP));
	}

}
