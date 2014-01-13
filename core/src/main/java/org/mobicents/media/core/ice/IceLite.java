package org.mobicents.media.core.ice;

import org.ice4j.Transport;
import org.ice4j.TransportAddress;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CompatibilityMode;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.harvest.StunCandidateHarvester;
import org.ice4j.ice.harvest.TurnCandidateHarvester;
import org.ice4j.ice.harvest.UPNPHarvester;
import org.ice4j.security.LongTermCredential;

/**
 * ICE Lite implementation using ice4j.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceLite {

	public static final String STUN_SERVER_JITSI = "stun.jitsi.net";
	public static final String STUN6_SERVER_JITSI = "stun6.jitsi.net";
	public static final String STUN_SERVER_JITSI_USERNAME = "guest";
	public static final String STUN_SERVER_JITSI_PASSWORD = "anonymouspower!!";

	// Port range for candidate harvesting
	private static final int MIN_PORT = 1025;
	private static final int MAX_PORT = 65534;

	// TODO This should be set in a configuration file - hrosa
	private boolean stunSupported = false;
	private boolean turnSupported = false;

	/**
	 * Indicates whether STUN is supported
	 * 
	 * @return
	 */
	public boolean isStunSupported() {
		return stunSupported;
	}

	/**
	 * Sets whether STUN is supported
	 * 
	 * @param stunSupported
	 */
	public void setStunSupported(boolean stunSupported) {
		this.stunSupported = stunSupported;
	}

	/**
	 * Indicates whether TURN is supported
	 * 
	 * @return
	 */
	public boolean isTurnSupported() {
		return turnSupported;
	}

	/**
	 * Sets whether TURN is supported
	 * 
	 * @param turnSupported
	 */
	public void setTurnSupported(boolean turnSupported) {
		this.turnSupported = turnSupported;
	}

	/**
	 * Creates an ICE <tt>Agent</tt> (vanilla or trickle, depending on the value
	 * of <tt>isTrickling</tt>) and adds to it an audio stream with RTP and RTCP
	 * components.
	 * 
	 * @param rtpPort
	 *            the port that we should try to bind the RTP component on (the
	 *            RTCP one would automatically go to rtpPort + 1)
	 * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
	 *         components.
	 * @param isTrickling
	 *            indicates whether the newly created agent should be performing
	 *            trickle ICE.
	 * @throws IceException
	 *             When the components for the media streams cannot be created.
	 * 
	 * @throws Throwable
	 *             if anything goes wrong.
	 */
	public Agent createAgent(int rtpPort, boolean isTrickling)
			throws IceException {
		Agent agent = new Agent(CompatibilityMode.RFC5245);
		agent.setTrickling(isTrickling);

		// TODO Dynamically change port -hrosa
		final int port = 3478;
		final Transport udpTransport = Transport.UDP;

		// STUN
		LongTermCredential longTermCredential = new LongTermCredential(
				STUN_SERVER_JITSI_USERNAME, STUN_SERVER_JITSI_PASSWORD);

		if (this.stunSupported) {
			StunCandidateHarvester stunHarv = new StunCandidateHarvester(
					new TransportAddress(STUN_SERVER_JITSI, port, udpTransport));
			agent.addCandidateHarvester(stunHarv);

			StunCandidateHarvester stun6Harv = new StunCandidateHarvester(
					new TransportAddress(STUN6_SERVER_JITSI, port, udpTransport));
			agent.addCandidateHarvester(stun6Harv);
		}

		if (this.turnSupported) {
			TurnCandidateHarvester turnHarv = new TurnCandidateHarvester(
					new TransportAddress(STUN_SERVER_JITSI, port, udpTransport),
					longTermCredential);
			agent.addCandidateHarvester(turnHarv);

			TurnCandidateHarvester turn6Harv = new TurnCandidateHarvester(
					new TransportAddress(STUN6_SERVER_JITSI, port, udpTransport),
					longTermCredential);
			agent.addCandidateHarvester(turn6Harv);
		}

		// UPnP: adding an UPnP harvester because they are generally slow which
		// makes it more convenient to test things like trickle.
		agent.addCandidateHarvester(new UPNPHarvester());

		// STREAMS
		createStream(rtpPort, "audio", agent);
		// TODO Add support for video - hrosa

		return agent;
	}

	/**
	 * Creates a Vanilla ICE <tt>Agent</tt> and adds to it an audio stream with
	 * RTP and RTCP components.
	 * 
	 * @param rtpPort
	 *            the port that we should try to bind the RTP component on (the
	 *            RTCP one would automatically go to rtpPort + 1)
	 * @return an ICE <tt>Agent</tt> with an audio stream with RTP and RTCP
	 *         components.
	 * 
	 * @throws IceException
	 *             if anything goes wrong.
	 */
	public Agent createAgent(int rtpPort) throws IceException {
		return this.createAgent(rtpPort, false);
	}

	/**
	 * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and and RTCP
	 * component.
	 * 
	 * @param rtpPort
	 *            the port that we should try to bind the RTP component on (the
	 *            RTCP one would automatically go to rtpPort + 1)
	 * @param streamName
	 *            the name of the stream to create
	 * @param agent
	 *            the <tt>Agent</tt> that should create the stream.
	 * 
	 * @return the newly created <tt>IceMediaStream</tt>.
	 * @throws IceException
	 *             if anything goes wrong.
	 */
	private IceMediaStream createStream(int rtpPort, String streamName,
			Agent agent) throws IceException {
		IceMediaStream stream = agent.createMediaStream(streamName);
		// TODO Validate port range - hrosa
		try {
			// rtp stream
			agent.createComponent(stream, Transport.UDP, rtpPort, rtpPort,
					rtpPort + 100);
			// rtcp stream
			agent.createComponent(stream, Transport.UDP, rtpPort + 1,
					rtpPort + 1, rtpPort + 101);
		} catch (Exception e) {
			throw new IceException(
					"Could not create component for media stream", e);
		}
		return stream;
	}
}
