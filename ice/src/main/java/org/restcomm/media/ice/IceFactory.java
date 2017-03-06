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

import org.restcomm.media.ice.lite.IceLiteAgent;

/**
 * A factory to produce ICE agents.<br>
 * The user can decide whether to create agents for full or lite ICE
 * implementations.
 * 
 * @author Henrique Rosa
 * 
 */
public class IceFactory {

	/**
	 * Produces a new agent that implements ICE-lite.<br>
	 * As such, this agent won't keep states nor attempt connectivity checks.<br>
	 * It will only listen to incoming connectivity checks.
	 * 
	 * @return The ice-lite agent
	 * @see <a href="http://tools.ietf.org/html/rfc5245#section-4.2">RFC5245</a>
	 */
	public static IceLiteAgent createLiteAgent() {
		return new IceLiteAgent();
	}
}
