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

	public HostCandidate(IceComponent component, InetAddress address, int port) {
		super(component, address, port, CandidateType.HOST);
		// A host candidate is also said to have a base, equal to itself.
		this.base = this;
	}

	public HostCandidate(IceComponent component, String hostname, int port) {
		super(component, hostname, port, CandidateType.HOST);
		// A host candidate is also said to have a base, equal to itself.
		this.base = this;
	}
}
