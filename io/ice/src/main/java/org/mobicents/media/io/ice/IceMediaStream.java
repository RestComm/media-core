package org.mobicents.media.io.ice;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class IceMediaStream {

	private final String name;
	private IceComponent rtpComponent;
	private IceComponent rtcpComponent;
	private String remoteUfrag;
	private String remotePassword;

	public IceMediaStream(String name) {
		this(name, true);
	}

	public IceMediaStream(String name, boolean rtcp) {
		validateName(name);
		this.name = name.toLowerCase();
		this.rtpComponent = new IceComponent(IceComponent.RTP_ID);
		if (rtcp) {
			this.rtcpComponent = new IceComponent(IceComponent.RTCP_ID);
		}
	}

	private void validateName(String name) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("The media stream name cannot be null or empty.");
		}
	}

	public String getName() {
		return name;
	}

	public IceComponent getRtpComponent() {
		return rtpComponent;
	}

	public boolean hasLocalRtpCandidates() {
		return !this.rtpComponent.getLocalCandidates().isEmpty();
	}

	public IceComponent getRtcpComponent() {
		return rtcpComponent;
	}

	public boolean hasLocalRtcpCandidates() {
		if (supportsRtcp()) {
			return !this.rtcpComponent.getLocalCandidates().isEmpty();
		}
		return false;
	}

	public boolean supportsRtcp() {
		return this.rtcpComponent != null;
	}

	public String getRemoteUfrag() {
		return remoteUfrag;
	}

	public String getRemotePassword() {
		return remotePassword;
	}

	/**
	 * Instructs both components of the Media Stream to select their local
	 * default candidates.
	 */
	public void selectLocalDefaultCandidates() {
		this.rtpComponent.selectDefaultLocalCandidate();
		if (this.supportsRtcp()) {
			this.rtcpComponent.selectDefaultLocalCandidate();
		}
	}
}
