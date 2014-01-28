package org.mobicents.media.core.ice;

import java.net.DatagramSocket;
import java.util.List;

import org.ice4j.Transport;
import org.ice4j.ice.Agent;
import org.ice4j.ice.CompatibilityMode;
import org.ice4j.ice.IceMediaStream;
import org.ice4j.ice.LocalCandidate;

public abstract class IceAgent {

	// Port range for candidate harvesting
	protected static final int MIN_PORT = 1025;
	protected static final int MAX_PORT = 65534;

	protected final Agent agent;
	
	protected IceAgent() {
		this.agent = new Agent(CompatibilityMode.RFC5245);
	}

	/**
	 * Checks whether the Agent implements ICE Lite
	 * 
	 * @return true if the agent implements ICE Lite. False, in case of full
	 *         ICE.
	 */
	public abstract boolean isLite();

	/**
	 * Checks whether the Agent is using ICE-trickle
	 * 
	 * @return true if agent uses trickle, false otherwise.
	 */
	public abstract boolean isTrickling();

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
	public IceMediaStream createStream(int port, String streamName)
			throws IceException {
		/*
		 * TODO Factory should keep track of used ports, to optimize harvesting
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
			agent.createComponent(stream, Transport.UDP, port, MIN_PORT,
					MAX_PORT);
			// FIXME Create RTCP component, when MMS supports it - hrosa
			// agent.createComponent(stream, Transport.UDP, rtpPort + 1,
			// rtpPort + 1, rtpPort + 1);
			return stream;
		} catch (Exception e) {
			throw new IceException(
					"Could not create component for media stream", e);
		}
	}

	public List<IceMediaStream> getStreams() {
		return this.agent.getStreams();
	}

	public String getLocalUfrag() {
		return this.agent.getLocalUfrag();
	}

	public String getLocalPassword() {
		return this.agent.getLocalPassword();
	}
	
	protected LocalCandidate getLocalCandidate(String stream) {
		return this.agent.getSelectedLocalCandidate(stream);
	}

	public DatagramSocket getUdpSocket(String stream) {
		LocalCandidate candidate = getLocalCandidate(stream);
		if(candidate != null) {
			return candidate.getDatagramSocket();
		}
		return null;
	}
}
