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

package org.restcomm.media.ice.network.stun;

import java.net.InetSocketAddress;

/**
 * Event that should be thrown when a STUN handler replies successfully to a
 * binding request.
 * 
 * @author Henrique Rosa
 * 
 */
public class BindingSuccessEvent {

	private final StunHandler source;
	private final InetSocketAddress localPeer;
	private final InetSocketAddress remotePeer;

	public BindingSuccessEvent(StunHandler source, InetSocketAddress localPeer, InetSocketAddress remotePeer) {
		super();
		this.source = source;
		this.localPeer = localPeer;
		this.remotePeer = remotePeer;
	}

	public StunHandler getSource() {
		return source;
	}

	public InetSocketAddress getLocalPeer() {
		return localPeer;
	}
	
	public InetSocketAddress getRemotePeer() {
		return remotePeer;
	}

}
