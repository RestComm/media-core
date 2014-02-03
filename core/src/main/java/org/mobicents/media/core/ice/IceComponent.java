package org.mobicents.media.core.ice;

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

	private List<IceCandidate> localCandidates;
	private IceCandidate defaultLocalCandidate;

	private List<IceCandidate> remoteCandidates;
	private IceCandidate defaultRemoteCandidate;

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

}
