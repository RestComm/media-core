package org.mobicents.media.core.ice;

import java.util.Collections;
import java.util.List;

import org.mobicents.media.core.ice.candidate.IceCandidate;
import org.mobicents.media.core.ice.candidate.LocalCandidateWrapper;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class IceComponent {

	public static final int RTP_ID = 1;
	public static final int RTCP_ID = 2;

	private int componentId;

	private List<LocalCandidateWrapper> localCandidates;
	private LocalCandidateWrapper defaultLocalCandidate;

	// private List<IceCandidate> remoteCandidates;
	// private IceCandidate defaultRemoteCandidate;

	public IceComponent(int componentId) {
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
	 * Attempts to registers a local candidate.
	 * 
	 * @param candidateWrapper
	 *            The wrapper that contains the local ICE candidate
	 */
	public void addLocalCandidate(LocalCandidateWrapper candidateWrapper) {
		IceCandidate candidate = candidateWrapper.getCandidate();

		// Configure the candidate before registration
		candidate.setPriority(calculatePriority(candidate));

		synchronized (this.localCandidates) {
			if (!this.localCandidates.contains(candidateWrapper)) {
				this.localCandidates.add(candidateWrapper);
				Collections.sort(this.localCandidates);
			}
		}
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
			addLocalCandidate(candidateWrapper);
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
}
