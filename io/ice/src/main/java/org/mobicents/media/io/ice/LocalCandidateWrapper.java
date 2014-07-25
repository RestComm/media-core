package org.mobicents.media.io.ice;

import java.nio.channels.DatagramChannel;

/**
 * Elects a {@link Candidate} as local.
 * 
 * @author Henrique Rosa
 * 
 */
public class LocalCandidateWrapper implements CandidateWrapper, Comparable<LocalCandidateWrapper> {

	private final IceCandidate candidate;
	private final DatagramChannel udpChannel;
	// TODO add stun stack to candidate wrapper
	
	
	public LocalCandidateWrapper(IceCandidate candidate,
			DatagramChannel udpChannel) {
		this.candidate = candidate;
		this.udpChannel = udpChannel;
	}

	public IceCandidate getCandidate() {
		return this.candidate;
	}

	public DatagramChannel getChannel() {
		return udpChannel;
	}

	public int compareTo(LocalCandidateWrapper other) {
		if(other == null) {
			return 1;
		}
		return this.candidate.compareTo(other.candidate);
	}

}
