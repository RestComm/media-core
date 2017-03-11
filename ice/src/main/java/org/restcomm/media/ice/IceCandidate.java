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

import java.net.InetAddress;

import org.restcomm.media.network.deprecated.TransportAddress;

/**
 * A Candidate is a transport address that is a potential point of contact for
 * receipt of media. <br>
 * Candidates also have properties -- their type (server reflexive, relayed or
 * host), priority, foundation, and base.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceCandidate extends TransportAddress implements Comparable<IceCandidate>, Cloneable {

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
	private String foundation;

	/**
	 * Each candidate <b>must</b> be assigned a unique priority amongst all
	 * candidates for the same media stream.
	 */
	private long priority;

	/**
	 * The base of a server reflexive candidate is the host candidate from which
	 * it was derived.<br>
	 * A host candidate is also said to have a base, equal to that candidate
	 * itself. Similarly, the base of a relayed candidate is that candidate
	 * itself.
	 */
	protected IceCandidate base;

	public IceCandidate(IceComponent component, InetAddress address, int port, final CandidateType type) {
		super(address, port, TransportProtocol.UDP);
		this.component = component;
		this.type = type;
		this.addressPrecedence = calculateAddressPrecedence();
		this.priority = 0;
	}

	protected IceCandidate(IceComponent component, String hostname, int port, final CandidateType type) {
		super(hostname, port, TransportProtocol.UDP);
		this.component = component;
		this.type = type;
		this.addressPrecedence = calculateAddressPrecedence();
		this.priority = 0;
	}

	public CandidateType getType() {
		return type;
	}

	public String getFoundation() {
		return foundation;
	}

	public void setFoundation(String foundation) {
		this.foundation = foundation;
	}

	public int getAddressPrecedence() {
		return addressPrecedence;
	}

	public long getPriority() {
		return priority;
	}

	public void setPriority(long priority) {
		this.priority = priority;
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
			throw new UnsupportedOperationException("IPv6 addresses are not supported.");
		}
		return IP4_PRECEDENCE;
	}

	public int compareTo(IceCandidate o) {
		if (o == null) {
			return 1;
		}
		return (int) (this.priority - o.priority);
	}

	@Override
	public IceCandidate clone() throws CloneNotSupportedException {
		return (IceCandidate) super.clone();
	}

	public short getComponentId() {
		return this.component.getComponentId();
	}
}
