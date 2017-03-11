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

package org.restcomm.media.ice.harvest;

import java.nio.channels.Selector;

import org.restcomm.media.ice.CandidateType;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public interface CandidateHarvester {

	/**
	 * Harvests candidates.
	 * 
	 * @param portManager
	 *            Manages the port range allowed for candidate harvesting.
	 * @param mediaStream
	 *            The media stream to bind the candidate to.
	 * @param selector
	 *            the selector that will be registered to the data channel of
	 *            the gathered candidates
	 * @throws HarvestException
	 *             When an error occurs during the candidate harvesting process.
	 */
	void harvest(RtpPortManager portManager, IceMediaStream mediaStream, Selector selector) throws HarvestException;

	/**
	 * Gets the type of candidates gathered by the harvester.
	 * 
	 * @return The type of harvested candidates
	 */
	CandidateType getCandidateType();

}
