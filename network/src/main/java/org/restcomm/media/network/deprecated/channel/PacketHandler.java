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

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface PacketHandler extends Comparable<PacketHandler> {

	/**
	 * Checks whether the handler can process the incoming packet or not.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @return <code>true</code>, if the packet can be handled.
	 *         <code>false</code>, otherwise.
	 */
	boolean canHandle(byte[] packet);

	boolean canHandle(byte[] packet, int dataLength, int offset);

	/**
	 * Processes the packet and provides a suitable answer.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @param localPeer
	 *            The local peer who received the packet
	 * @param remotePeer
	 *            The remote peer who sent the packet
	 * @return The answer to be sent to the remote peer as response to the
	 *         incoming packet.
	 * @throws PacketHandlerException
	 *             When the handler cannot process the packet.
	 */
	byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException;

	/**
	 * Processes the packet and provides a suitable answer.
	 * 
	 * @param packet
	 *            The packet to be processed.
	 * @param dataLength
	 *            The length of the data to be read.
	 * @param offset
	 *            The initial position to start reading data from.
	 * @param localPeer
	 *            The local peer who received the packet
	 * @param remotePeer
	 *            The remote peer who sent the packet
	 * @return The answer to be sent to the remote peer as response to the
	 *         incoming packet.
	 * @throws PacketHandlerException
	 *             When the handler cannot process the packet.
	 */
	byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException;

	/**
	 * Gets the priority of the handler in the pipeline.<br>
	 * The priority affects the place of the handler in the pipeline. This can
	 * affect performance as handlers with higher priority will be queried first
	 * when a packet arrives.
	 * 
	 * @return The priority of the handler
	 */
	int getPipelinePriority();

}
