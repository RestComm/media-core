package org.mobicents.media.core.ice;

import java.net.InetAddress;

public class ServerReflexiveCandidate extends IceCandidate {

	private static final long serialVersionUID = -8842684132867302905L;

	public ServerReflexiveCandidate(IceComponent component, InetAddress address, int port, HostCandidate base) {
		super(component, address, port, CandidateType.SRFLX);
		this.base = base;
	}

	public ServerReflexiveCandidate(IceComponent component, String hostname, int port, HostCandidate base) {
		super(component, hostname, port, CandidateType.SRFLX);
		this.base = base;
	}

}
