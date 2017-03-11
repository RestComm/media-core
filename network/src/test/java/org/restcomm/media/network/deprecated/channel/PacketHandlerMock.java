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

package org.restcomm.media.network.deprecated.channel;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.restcomm.media.network.deprecated.channel.PacketHandler;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;

/**
 * A mock of the Packet Handler.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class PacketHandlerMock implements PacketHandler {

	private int priority;
	private String validDataString;
	private byte[] validData;
	
	public PacketHandlerMock(int priority, String validData) {
		this.priority = priority;
		this.validDataString = validData;
		this.validData = this.validDataString.getBytes();
	}
	
	@Override
	public int compareTo(PacketHandler o) {
		if(o == null) {
			return 1;
		}
		return this.priority - o.getPipelinePriority();
	}

	@Override
	public boolean canHandle(byte[] packet) {
		return canHandle(packet, packet.length, 0);
	}

	@Override
	public boolean canHandle(byte[] packet, int dataLength, int offset) {
		byte[] dest = Arrays.copyOfRange(packet, offset, dataLength);
		return Arrays.equals(this.validData, dest);
	}

	@Override
	public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		return handle(packet, packet.length, 0, localPeer, remotePeer);
	}

	@Override
	public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		String request = new String(Arrays.copyOfRange(packet, offset, dataLength));
		return ("received " + request).getBytes();
	}

	@Override
	public int getPipelinePriority() {
		return this.priority;
	}

}
