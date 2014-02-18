package org.mobicents.media.core.ice;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class IceComponent {

	public static final int RTP_ID = 1;
	public static final int RTCP_ID = 2;

	private int componentId;

	private final List<LocalCandidateWrapper> localCandidates;
	private LocalCandidateWrapper defaultLocalCandidate;
	private final List<IceCandidate> remoteCandidates;
	private IceCandidate defaultRemoteCandidate;

	private CandidatePair candidatePair;

	public IceComponent(int componentId) {
		this.localCandidates = new ArrayList<LocalCandidateWrapper>(5);
		this.remoteCandidates = new ArrayList<IceCandidate>(5);
		setComponentId(componentId);
	}

	/**
	 * Validates whether a value can be used as an ICE Component ID.
	 * <p>
	 * For RTP components, the ID must be 1.<br>
	 * For RTCP components, the ID must be 2.
	 * </p>
	 * 
	 * @param componentId
	 *            The value to be evaluated.
	 * @return <code>true</code> if its a valid Component ID. Returns
	 *         <code>false</code> otherwise.
	 */
	private boolean isValidComponentId(int componentId) {
		if (componentId != RTP_ID && componentId != RTCP_ID) {
			return false;
		}
		return true;
	}

	public int getComponentId() {
		return componentId;
	}

	public void setComponentId(int componentId) {
		if (!isValidComponentId(componentId)) {
			throw new IllegalArgumentException("Invalid Component ID: "
					+ componentId);
		}
		this.componentId = componentId;
	}

	/**
	 * Registers a collection of local candidates to the component.
	 * 
	 * @param candidatesWrapper
	 *            The list of local candidates
	 * 
	 * @see IceComponent#addLocalCandidate(LocalCandidateWrapper)
	 */
	public void addLocalCandidates(List<LocalCandidateWrapper> candidatesWrapper) {
		for (LocalCandidateWrapper candidateWrapper : candidatesWrapper) {
			addLocalCandidate(candidateWrapper, false);
		}
		sortCandidates();
	}

	/**
	 * Attempts to registers a local candidate.
	 * 
	 * @param candidateWrapper
	 *            The wrapper that contains the local ICE candidate
	 */
	public void addLocalCandidate(LocalCandidateWrapper candidateWrapper) {
		this.addLocalCandidate(candidateWrapper, true);
	}

	/**
	 * Attempts to registers a local candidate.
	 * 
	 * @param candidateWrapper
	 *            The wrapper that contains the local ICE candidate
	 * @param sort
	 *            Decides whether the candidates list should be ordered after
	 *            insert
	 */
	private void addLocalCandidate(LocalCandidateWrapper candidateWrapper,
			boolean sort) {
		IceCandidate candidate = candidateWrapper.getCandidate();

		// Configure the candidate before registration
		candidate.setPriority(calculatePriority(candidate));

		synchronized (this.localCandidates) {
			if (!this.localCandidates.contains(candidateWrapper)) {
				this.localCandidates.add(candidateWrapper);
				sortCandidates();
			}
		}
	}

	private void sortCandidates() {
		synchronized (localCandidates) {
			Collections.sort(this.localCandidates);
		}
	}

	/**
	 * Calculates the priority of a candidate, using the following formula:
	 * <p>
	 * <code>
	 * p=(2^24 * candidate type preference) + (2^8 * IP precedence) + (2^0 * (256 -
	 * component ID))
	 * </code>
	 * </p>
	 */
	private long calculatePriority(IceCandidate candidate) {
		return (long) (candidate.getType().getPreference() << 24)
				+ (long) (candidate.getAddressPrecedence() << 8)
				+ (long) (256 - this.getComponentId());
	}

	public LocalCandidateWrapper selectDefaultLocalCandidate() {
		// Choose the candidate with greatest priority
		// This is fine because this implementation only supports IPv4 addresses
		// That being said, it should only be one address. If there are more,
		// let priority decide.
		this.defaultLocalCandidate = this.localCandidates.get(0);
		return this.defaultLocalCandidate;
	}

	public List<LocalCandidateWrapper> getLocalCandidates() {
		List<LocalCandidateWrapper> copy;
		synchronized (this.localCandidates) {
			copy = new ArrayList<LocalCandidateWrapper>(this.localCandidates);
		}
		return copy;
	}

	public LocalCandidateWrapper getDefaultLocalCandidate() {
		return defaultLocalCandidate;
	}

	public boolean isDefaultLocalCandidateSelected() {
		return this.defaultLocalCandidate != null;
	}

	public CandidatePair getSelectedCandidates() {
		return candidatePair;
	}

	public CandidatePair setCandidatePair(SelectionKey key) {
		for (LocalCandidateWrapper localCandidate : getLocalCandidates()) {
			if (key.channel().equals(localCandidate.getChannel())) {
				this.candidatePair = new CandidatePair(componentId, key);
				return this.candidatePair;
			}
		}
		return null;
	}
}
