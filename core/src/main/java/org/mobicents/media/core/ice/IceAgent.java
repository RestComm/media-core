package org.mobicents.media.core.ice;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mobicents.media.core.ice.harvest.HarvestException;
import org.mobicents.media.core.ice.harvest.HarvestManager;
import org.mobicents.media.core.ice.harvest.NoCandidatesGatheredException;
import org.mobicents.media.core.ice.network.stun.ConnectivityCheckServer;
import org.mobicents.media.core.ice.security.IceAuthenticator;

public abstract class IceAgent implements IceAuthenticator {

	private final Map<String, IceMediaStream> mediaStreams;
	private final HarvestManager harvestManager;

	protected final String ufrag;
	protected final String password;

	protected Selector selector;
	protected ConnectivityCheckServer connectivityCheckServer;
	
	private DatagramChannel selectedChannel;

	protected IceAgent() {
		this.mediaStreams = new LinkedHashMap<String, IceMediaStream>(5);
		this.harvestManager = new HarvestManager();

		SecureRandom random = new SecureRandom();
		this.ufrag = new BigInteger(24, random).toString(32);
		this.password = new BigInteger(128, random).toString(32);
	}

	/**
	 * Checks whether the Agent implements ICE Lite
	 * 
	 * @return true if the agent implements ICE Lite. False, in case of full
	 *         ICE.
	 */
	public abstract boolean isLite();

	/**
	 * Checks whether the Agent is controlling the ICE process.
	 * 
	 * @return
	 */
	public abstract boolean isControlling();

	/**
	 * Gets the local user fragment.
	 * 
	 * @return the local <code>ice-ufrag</code>
	 */
	public String getUfrag() {
		return ufrag;
	}

	/**
	 * Gets the password of the local user fragment
	 * 
	 * @return the local <code>ice-pwd</code>
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Creates an <tt>IceMediaStream</tt> and adds to it an RTP and an RTCP
	 * components.
	 * 
	 * @param streamName
	 *            the name of the stream to create
	 * @param agent
	 *            the <tt>Agent</tt> that should create the stream.
	 * 
	 * @return the newly created <tt>IceMediaStream</tt>.
	 */
	public IceMediaStream addMediaStream(String streamName) {
		return addMediaStream(streamName, true);
	}

	/**
	 * Creates and registers a new media stream with an RTP component.<br>
	 * An secondary component may be created if the stream supports RTCP.
	 * 
	 * @param streamName
	 *            the name of the media stream
	 * @param rtcp
	 *            Indicates whether the media server supports RTCP.
	 * @return The newly created media stream.
	 */
	public IceMediaStream addMediaStream(String streamName, boolean rtcp) {
		return this.mediaStreams.put(streamName, new IceMediaStream(streamName,
				rtcp));
	}

	/**
	 * Gets a media stream by name
	 * 
	 * @param streamName
	 *            The name of the media stream
	 * @return The media stream. Returns null, if no media stream exists with
	 *         such name.
	 */
	public IceMediaStream getMediaStream(String streamName) {
		IceMediaStream mediaStream;
		synchronized (mediaStreams) {
			mediaStream = this.mediaStreams.get(streamName);
		}
		return mediaStream;
	}

	public List<IceMediaStream> getMediaStreams() {
		List<IceMediaStream> copy;
		synchronized (mediaStreams) {
			copy = new ArrayList<IceMediaStream>(this.mediaStreams.values());
		}
		return copy;
	}

	/**
	 * Gathers all available candidates and sets the components of each media
	 * stream.
	 * 
	 * @param preferredPort
	 *            The preferred port to bind candidates to
	 * @throws HarvestException
	 *             An error occurred while harvesting candidates
	 */
	public void gatherCandidates(int preferredPort) throws HarvestException,
			NoCandidatesGatheredException {
		// Initialize the selector if necessary
		if (this.selector == null) {
			try {
				this.selector = Selector.open();
			} catch (IOException e) {
				throw new HarvestException("Could not initialize selector", e);
			}
		}

		// Gather candidates for each media stream
		for (IceMediaStream mediaStream : getMediaStreams()) {
			this.harvestManager.harvest(mediaStream, preferredPort,
					this.selector);
		}
	}

	/**
	 * Starts the ICE agent by activating its STUN stack.
	 * <p>
	 * <b>Full</b> ICE implementations start connectivity checks while listening
	 * for incoming checks.<br>
	 * <b>Lite</b> implementations are restricted to listen to incoming
	 * connectivity checks.
	 * </p>
	 */
	public abstract void start();

	public void selectChannel(SelectionKey key) {
		this.selectedChannel = (DatagramChannel) key.channel();
	}
	
	public DatagramChannel getSelectedChannel() {
		return selectedChannel;
	}

	public byte[] getLocalKey(String ufrag) {
		if (isUserRegistered(ufrag)) {
			if (this.password != null) {
				return this.password.getBytes();
			}
		}
		return null;
	}

	public byte[] getRemoteKey(String ufrag, String media) {
		// Verify if media stream exists
		IceMediaStream stream = getMediaStream(media);
		if (stream == null) {
			return null;
		}

		// Check whether full username is provided or just the fragment
		int colon = ufrag.indexOf(":");
		if (colon < 0) {
			if (ufrag.equals(stream.getRemoteUfrag())) {
				return stream.getRemotePassword().getBytes();
			}
		} else {
			if (ufrag.equals(getLocalUsername(stream))) {
				if (stream.getRemotePassword() != null) {
					return stream.getRemotePassword().getBytes();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the user name that the ICE Agent should use in connectivity
	 * checks for outgoing Binding Requests. According to RFC 5245, a Binding
	 * Request serving as a connectivity check MUST utilize the STUN short term
	 * credential mechanism. The username for the credential is formed by
	 * concatenating the username fragment provided by the peer with the
	 * username fragment of the agent sending the request, separated by a colon
	 * (":"). The password is equal to the password provided by the peer. For
	 * example, consider the case where agent L is the offerer, and agent R is
	 * the answerer. Agent L included a username fragment of LFRAG for its
	 * candidates, and a password of LPASS. Agent R provided a username fragment
	 * of RFRAG and a password of RPASS. A connectivity check from L to R (and
	 * its response of course) utilize the username RFRAG:LFRAG and a password
	 * of RPASS. A connectivity check from R to L (and its response) utilize the
	 * username LFRAG:RFRAG and a password of LPASS.
	 * 
	 * @param media
	 *            media name that we want to generate local username for.
	 * @return a user name that this <tt>Agent</tt> can use in connectivity
	 *         check for outgoing Binding Requests.
	 */
	private String getLocalUsername(IceMediaStream stream) {
		if (stream != null) {
			if (stream.getRemotePassword() != null) {
				return this.ufrag + ":" + stream.getRemotePassword();
			}
		}
		return null;

	}

	public boolean isUserRegistered(String ufrag) {
		int colon = ufrag.indexOf(":");
		String result = colon < 0 ? ufrag : ufrag.substring(0, colon);
		return result.equals(this.ufrag);
	}
}
