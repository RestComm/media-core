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

import java.net.InetAddress;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import org.restcomm.media.ice.CandidateType;
import org.restcomm.media.ice.FoundationsRegistry;
import org.restcomm.media.ice.IceMediaStream;
import org.restcomm.media.ice.lite.LiteFoundationsRegistry;
import org.restcomm.media.network.deprecated.RtpPortManager;

/**
 * Manages the candidate harvesting process
 * 
 * @author Henrique Rosa
 * 
 */
public class HarvestManager {

	private final FoundationsRegistry foundations;
	private final Map<CandidateType, CandidateHarvester> harvesters;
	private InetAddress externalAddress;

	public HarvestManager() {
		this.foundations = new LiteFoundationsRegistry();
		this.harvesters = new HashMap<CandidateType, CandidateHarvester>(
				CandidateType.count());
		this.harvesters.put(CandidateType.HOST, new HostCandidateHarvester(
				this.foundations));
	}

	/**
	 * Registers a harvester for a certain type of candidates. <b> Only
	 * <b>one</b> harvester per candidate type is supported.
	 * 
	 * @param harvester
	 *            The harvester to be registered
	 * @return Whether the harvester was successfully registered or not.
	 */
	public boolean addHarvester(CandidateHarvester harvester) {
		CandidateType candidateType = harvester.getCandidateType();
		boolean added = false;
		synchronized (this.harvesters) {
			if (!this.harvesters.containsKey(candidateType)) {
				this.harvesters.put(candidateType, harvester);
				added = true;
			} else {
				added = false;
			}
		}
		return added;
	}

	public CandidateHarvester removeHarvester(CandidateType type) {
		synchronized (this.harvesters) {
			return this.harvesters.remove(type);
		}
	}

	/**
	 * Gets the foundations registry managed during the lifetime of the ICE
	 * agent.
	 * 
	 * @return The foundations registry
	 */
	public FoundationsRegistry getFoundationsRegistry() {
		return this.foundations;
	}

	public InetAddress getExternalAddress() {
		return externalAddress;
	}

	public void harvest(IceMediaStream mediaStream, RtpPortManager portManager, Selector selector) throws HarvestException, NoCandidatesGatheredException {
		// Ask each harvester to gather candidates for the media stream
		// HOST candidates take precedence and are mandatory
		CandidateHarvester hostHarvester = harvesters.get(CandidateType.HOST);
		if (hostHarvester != null) {
			hostHarvester.harvest(portManager, mediaStream, selector);
		} else {
			throw new HarvestException("No HOST harvester registered!");
		}
		// Then comes the SRFLX, which depends on HOST candidates
		CandidateHarvester srflxHarvester = harvesters.get(CandidateType.SRFLX);
		if (srflxHarvester != null) {
			srflxHarvester.harvest(portManager, mediaStream, selector);
		}
		// RELAY candidates come last
		CandidateHarvester relayHarvester = harvesters.get(CandidateType.RELAY);
		if (relayHarvester != null) {
			relayHarvester.harvest(portManager, mediaStream, selector);
		}

		// Verify at least one candidate was gathered
		if (!mediaStream.hasLocalRtpCandidates()) {
			throw new NoCandidatesGatheredException("No RTP candidates were gathered for " + mediaStream.getName() + " stream");
		}

		// After harvesting all possible candidates, ask the media stream to
		// select its default local candidates
		mediaStream.getRtpComponent().selectDefaultLocalCandidate();
		if (mediaStream.supportsRtcp() && !mediaStream.isRtcpMux()) {
			mediaStream.getRtcpComponent().selectDefaultLocalCandidate();
		}
	}

}
