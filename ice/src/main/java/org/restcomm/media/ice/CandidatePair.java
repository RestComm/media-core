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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class CandidatePair {

	// TODO add references to local and remote candidate

	private final DatagramChannel channel;
	private final int componentId;

	public CandidatePair(int componentId, DatagramChannel channel) {
		this.componentId = componentId;
		this.channel = channel;
	}

	public int getComponentId() {
		return this.componentId;
	}

	public DatagramChannel getChannel() {
		return this.channel;
	}

	public int getLocalPort() {
		try {
			return ((InetSocketAddress) this.channel.getLocalAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getLocalAddress() {
		try {
			return ((InetSocketAddress) this.channel.getLocalAddress()).getHostName();
		} catch (IOException e) {
			return "";
		}
	}

	public int getRemotePort() {
		try {
			return ((InetSocketAddress) this.channel.getRemoteAddress()).getPort();
		} catch (IOException e) {
			return 0;
		}
	}

	public String getRemoteAddress() {
		try {
			return ((InetSocketAddress) this.channel.getRemoteAddress()).getHostName();
		} catch (IOException e) {
			return "";
		}
	}

}
