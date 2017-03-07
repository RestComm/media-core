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

package org.restcomm.media.ice.lite;

import org.restcomm.media.ice.FoundationsRegistry;
import org.restcomm.media.ice.IceCandidate;

/**
 * Registry that keeps records of generated foundations for the lifetime of an
 * ICE agent.
 * 
 * <p>
 * Each candidate is assigned a foundation. The foundation MUST be different for
 * two candidates allocated from different IP addresses, and MUST be the same
 * otherwise.
 * </p>
 * 
 * <p>
 * For a lite implementation, a simple integer that increments for each IP
 * address will suffice
 * </p>
 * 
 * @author Henrique Rosa
 * 
 */
public class LiteFoundationsRegistry extends FoundationsRegistry {

	@Override
	public String computeIdentifier(IceCandidate candidate) {
		return candidate.getAddress().getHostAddress();
	}
}
