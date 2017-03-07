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
