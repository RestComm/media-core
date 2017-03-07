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

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that keeps records of generated foundations for the lifetime of an
 * ICE agent.
 * 
 * @author Henrique Rosa
 * @see <a href="http://tools.ietf.org/html/rfc5245#section-4">RFC5245</a>
 */
public abstract class FoundationsRegistry {

	/**
	 * Map that associates addresses to their corresponding foundation
	 */
	Map<String, String> foundations = new HashMap<String, String>();

	private int currentFoundation = 0;

	/**
	 * Retrieves the foundation associated to an address.<br>
	 * If no foundation exists for such address, one will be generated.
	 * 
	 * @param candidate
	 *            The ICE candidate
	 * @return The foundation associated to the candidate address.
	 */
	public String assignFoundation(IceCandidate candidate) {
		String identifier = computeIdentifier(candidate);
		synchronized (this.foundations) {
			String foundation = this.foundations.get(identifier);
			if (foundation == null) {
				this.currentFoundation++;
				foundation = String.valueOf(this.currentFoundation);
				this.foundations.put(identifier, foundation);
			}
			candidate.setFoundation(foundation);
			return foundation;
		}
	}

	/**
	 * Computes the identifier, based on a candidate, that will be used as the
	 * identifier to a foundation.
	 * 
	 * @param candidate
	 *            The candidate that holds data that will form the identifier
	 * @return The identifier
	 */
	protected abstract String computeIdentifier(IceCandidate candidate);

}
