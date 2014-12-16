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

package org.mobicents.media.server.io.network.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Pipeline that selects a capable {@link PacketHandler} to process incoming
 * packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketHandlerPipeline {

	private static final Comparator<PacketHandler> REVERSE_COMPARATOR = new Comparator<PacketHandler>() {
		
		@Override
		public int compare(PacketHandler o1, PacketHandler o2) {
			return o2.compareTo(o1);
		}
	};
	
	private final List<PacketHandler> handlers;
	private int count;

	public PacketHandlerPipeline() {
		this.handlers = new ArrayList<PacketHandler>(5);
		this.count = 0;
	}

	/**
	 * Registers a new packet handler in the pipeline.<br>
	 * Cannot register same handler twice.
	 * 
	 * @param handler
	 *            The handler to be registered.
	 * @return Whether the handler was successfully registered or not.
	 */
	public boolean addHandler(PacketHandler handler) {
		synchronized (this.handlers) {
			if (!handlers.contains(handler)) {
				handlers.add(handler);
				this.count++;
				Collections.sort(this.handlers, REVERSE_COMPARATOR);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Removes an existing packet handler from the pipeline.
	 * 
	 * @param handler
	 *            The handler to be removed
	 * @return Returns true if the handler is removed successfully. Returns
	 *         false, if the handler is not registered in the pipeline.
	 */
	public boolean removeHandler(PacketHandler handler) {
		synchronized (this.handlers) {
			boolean removed = this.handlers.remove(handler);
			if(removed) {
				this.count--;
			}
			return removed;
		}
	}

	/**
	 * Gets the number of handlers registered in the pipeline.
	 * 
	 * @return The number of registered handlers.
	 */
	public int count() {
		return this.count;
	}

	/**
	 * Checks whether a certain handler is already registered in the pipeline.
	 * 
	 * @param handler
	 *            The handler to look for
	 * @return <code>true</code> if the handler is registered. Returns
	 *         <code>false</code>, otherwise.
	 */
	public boolean contains(PacketHandler handler) {
		synchronized (this.handlers) {
			return this.handlers.contains(handler);
		}
	}

	/**
	 * Gets the protocol handler capable of processing the packet.
	 * 
	 * @param packet
	 *            The packet to be processed
	 * @return The protocol handler capable of processing the packet.<br>
	 *         Returns null in case no capable handler exists.
	 */
	public PacketHandler getHandler(byte[] packet) {
		// Copy the current handlers for thread safety
		List<PacketHandler> handlersCopy;
		synchronized (this.handlers) {
			handlersCopy = new ArrayList<PacketHandler>(this.handlers);
		}

		// Search for the first handler capable of processing the packet
		for (PacketHandler protocolHandler : handlersCopy) {
			if (protocolHandler.canHandle(packet)) {
				return protocolHandler;
			}
		}

		// Return null in case no handler is capable of decoding the packet
		return null;
	}
	
	/**
	 * Gets a <b>copy</b> of the handlers registered in the pipeline.
	 * 
	 * @return The list of handlers registered.
	 */
	public List<PacketHandler> getHandlers() {
		synchronized (this.handlers) {
			return new ArrayList<PacketHandler>(this.handlers);
		}
	}
	
}
