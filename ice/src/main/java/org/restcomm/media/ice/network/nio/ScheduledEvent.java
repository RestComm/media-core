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

package org.restcomm.media.ice.network.nio;

import java.nio.channels.DatagramChannel;

/**
 * Represent an event that is scheduled to be fired on a Server.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ScheduledEvent {

	private NioServer server;
	private DatagramChannel channel;
	private byte[] data;

	public ScheduledEvent(NioServer server, DatagramChannel channel, byte[] data) {
		super();
		this.server = server;
		this.channel = channel;
		this.data = data;
	}

	public void launch() {
		this.server.send(channel, data);
	}

}
