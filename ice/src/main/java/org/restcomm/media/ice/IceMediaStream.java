/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.ice;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class IceMediaStream {

	private final String name;
	private IceComponent rtpComponent;
	private IceComponent rtcpComponent;
	private String remoteUfrag;
	private String remotePassword;
	private boolean rtcpMux;

	public IceMediaStream(String name) {
		this(name, true);
	}

	public IceMediaStream(String name, boolean rtcp) {
		this(name, rtcp, false);
	}

	public IceMediaStream(String name, boolean rtcp, boolean rtcpMux) {
		validateName(name);
		this.name = name.toLowerCase();
		this.rtpComponent = new IceComponent(IceComponent.RTP_ID);
		if (rtcp) {
			this.rtcpComponent = new IceComponent(IceComponent.RTCP_ID);
		}
		this.rtcpMux = rtcp ? rtcpMux : false;
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
	
	public boolean isRtcpMux() {
		return this.rtcpMux;
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
