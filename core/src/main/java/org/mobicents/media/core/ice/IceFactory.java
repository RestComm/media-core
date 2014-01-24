package org.mobicents.media.core.ice;

import org.ice4j.Transport;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CompatibilityMode;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.NominationStrategy;

/**
 * A factory to produce ICE agents.<br>
 * The user can decide whether to create agents for full or lite ICE
 * implementations.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceFactory {

	// Port range for candidate harvesting
	private static final int MIN_PORT = 1025;
	private static final int MAX_PORT = 65534;

	public static Agent createLiteAgent(int port) throws IceException {
		Agent agent = new Agent(CompatibilityMode.RFC5245);
		agent.setTrickling(false);
		agent.setControlling(false);
		agent.setNominationStrategy(NominationStrategy.NOMINATE_HIGHEST_PRIO);

		// Media streams
		createStream(port, "audio", agent);
		return agent;
	}

	public static Agent createAgent(int port, boolean isControlling,
			boolean isTrickling) throws IceException {
		throw new UnsupportedOperationException("Full ICE is not implemented!");
	}

	/**
	 * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and and RTCP
	 * component.
	 * 
	 * @param port
	 *            the preferred port that we should try to bind the RTP
	 *            component on (the RTCP one would automatically go to rtpPort +
	 *            1)
	 * @param streamName
	 *            the name of the stream to create
	 * @param agent
	 *            the <tt>Agent</tt> that should create the stream.
	 * 
	 * @return the newly created <tt>IceMediaStream</tt>.
	 * @throws IceException
	 *             if anything goes wrong.
	 * @throws IllegalArgumentException
	 *             When a stream with <code>streamName</code> already exists.
	 */
	public static IceMediaStream createStream(int port, String streamName,
			Agent agent) throws IceException {
		/*
		 * TODO Factory should keep track of used ports, to shorten harvesting
		 * time - hrosa
		 */

		if (agent.getStreamNames().contains(streamName)) {
			throw new IllegalArgumentException("The stream " + streamName
					+ " already exists!");
		}
		
		try {
			// Create media stream
			IceMediaStream stream = agent.createMediaStream(streamName);

			// Create RTP/RTCP components for the media stream
			agent.createComponent(stream, Transport.UDP, MIN_PORT, port,
					MAX_PORT);
			// FIXME When RTCP is supported, the port should be RTPport+1 -hrosa
			// agent.createComponent(stream, Transport.UDP, rtpPort - 1,
			// rtpPort - 1, rtpPort - 1);
			return stream;
		} catch (Exception e) {
			throw new IceException(
					"Could not create component for media stream", e);
		}
	}
}
