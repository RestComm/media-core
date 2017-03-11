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

/**
 * Exception that is usually thrown when a {@link PacketHandler} cannot handle
 * an incoming packet.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketHandlerException extends Exception {

	private static final long serialVersionUID = -7774399750471780984L;

	public PacketHandlerException() {
		super();
	}

	public PacketHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public PacketHandlerException(String message) {
		super(message);
	}

	public PacketHandlerException(Throwable cause) {
		super(cause);
	}
	
}
