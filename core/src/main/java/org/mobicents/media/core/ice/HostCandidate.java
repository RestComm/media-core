package org.mobicents.media.core.ice;

import java.net.InetAddress;

/**
 * A candidate obtained by binding to a specific port from an IP address on the
 * host.
 * <p>
 * This includes IP addresses on physical interfaces and logical ones, such as
 * ones obtained through Virtual Private Networks (VPNs) and Realm Specific IP
 * (RSIP) [<a href="http://tools.ietf.org/html/rfc3102">RFC3102</a>] (which
 * lives at the operating system level).
 * </p>
 * 
 * @author Henrique Rosa
 * 
 */
public class HostCandidate extends IceCandidate {

	private static final long serialVersionUID = 1321731383535837192L;

	public HostCandidate(IceComponent component, InetAddress address, int port,
			String foundation) {
		super(component, address, port, CandidateType.HOST, foundation);
		// A host candidate is also said to have a base, equal to itself.
		this.base = this;
	}

	public HostCandidate(IceComponent component, String hostname, int port,
			String foundation) {
		super(component, hostname, port, CandidateType.HOST, foundation);
		// A host candidate is also said to have a base, equal to itself.
		this.base = this;
	}
}
