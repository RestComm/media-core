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
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class OperationRequest {

	public static final int REGISTER = 1;
	public static final int CHANGE_OPS = 2;

	public final DatagramChannel channel;
	public final int requestType;
	public final int operations;

	public OperationRequest(DatagramChannel channel, int type, int operations) {
		this.channel = channel;
		this.requestType = type;
		this.operations = operations;
	}

	public int getRequestType() {
		return requestType;
	}

	public int getOperations() {
		return operations;
	}

	public DatagramChannel getChannel() {
		return channel;
	}

}
