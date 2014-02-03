package org.mobicents.media.core.ice;

import java.net.InetAddress;

/**
 * A Candidate is a transport address that is a potential point of contact for
 * receipt of media. <br>
 * Candidates also have properties -- their type (server reflexive, relayed or
 * host), priority, foundation, and base.
 * 
 * @author Henrique Rosa
 * 
 */
public abstract class IceCandidate extends TransportAddress implements
		Comparable<IceCandidate> {

	private static final long serialVersionUID = 2306608901365110071L;

	public static final int IP4_PRECEDENCE = 65535;

	private final IceComponent component;
	private final int addressPrecedence;

	/**
	 * The ICE specification defines for candidate types: host, server
	 * reflexive, peer reflexive and relayed candidates.
	 */
	private final CandidateType type;

	/**
	 * An arbitrary string that is the same for two candidates that have the
	 * same type, base IP address, protocol (UDP, TCP, etc.), and STUN or TURN
	 * server. <br>
	 * If any of these are different, then the foundation will be different.
	 * <p>
	 * Two candidate pairs with the same foundation pairs are likely to have
	 * similar network characteristics. Foundations are used in the frozen
	 * algorithm.
	 * </p>
	 */
	private final String foundation;

	/**
	 * Each candidate <b>must</b> be assigned a unique priority amongst all
	 * candidates for the same media stream.
	 */
	private final long priority;

	/**
	 * The base of a server reflexive candidate is the host candidate from which
	 * it was derived.<br>
	 * A host candidate is also said to have a base, equal to that candidate
	 * itself. Similarly, the base of a relayed candidate is that candidate
	 * itself.
	 */
	protected IceCandidate base;

	protected IceCandidate(final IceComponent component,
			final InetAddress address, int port, final CandidateType type,
			String foundation) {
		super(address, port, TransportProtocol.UDP);
		this.component = component;
		this.type = type;
		this.addressPrecedence = calculateAddressPrecedence();
		this.priority = calculatePriority();
		this.foundation = foundation;
	}

	protected IceCandidate(final IceComponent component, final String hostname,
			int port, final CandidateType type, String foundation) {
		super(hostname, port, TransportProtocol.UDP);
		this.component = component;
		this.type = type;
		this.addressPrecedence = calculateAddressPrecedence();
		this.priority = calculatePriority();
		this.foundation = foundation;
	}

	public CandidateType getType() {
		return type;
	}

	public String getFoundation() {
		return foundation;
	}

	public int getAddressPrecedence() {
		return addressPrecedence;
	}

	public long getPriority() {
		return priority;
	}

	public IceCandidate getBase() {
		return base;
	}

	/**
	 * Calculates the address precedence of the candidate.
	 * 
	 * <p>
	 * <b>note:</b>For IPv4-only hosts, this values is always set to 65535. For
	 * IPv6 addresses, please refer to <a
	 * href="http://tools.ietf.org/html/rfc3484">RFC3484</a>
	 * </p>
	 * 
	 * @return The precedence value of the transport address
	 */
	private int calculateAddressPrecedence() {
		if (this.isIPv6()) {
			throw new UnsupportedOperationException(
					"IPv6 addresses are not supported.");
		}
		return IP4_PRECEDENCE;
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
	private long calculatePriority() {
		return (long) (this.type.getPreference() << 24)
				+ (long) (this.calculateAddressPrecedence() << 8)
				+ (long) (256 - this.component.getComponentId());
	}

	public int compareTo(IceCandidate o) {
		if (o == null) {
			return 1;
		}
		return (int) (this.priority - o.priority);
	}
}
