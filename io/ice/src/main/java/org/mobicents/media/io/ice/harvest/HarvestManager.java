package org.mobicents.media.io.ice.harvest;

import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;

import org.mobicents.media.io.ice.CandidateType;
import org.mobicents.media.io.ice.FoundationsRegistry;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.io.ice.lite.LiteFoundationsRegistry;
import org.mobicents.media.server.io.network.PortManager;

/**
 * Manages the candidate harvesting process
 * 
 * @author Henrique Rosa
 * 
 */
public class HarvestManager {
	
	private final FoundationsRegistry foundations;
	private final Map<CandidateType, CandidateHarvester> harvesters;

	public HarvestManager() {
		this.foundations = new LiteFoundationsRegistry();
		this.harvesters = new HashMap<CandidateType, CandidateHarvester>(CandidateType.count());
		this.harvesters.put(CandidateType.HOST, new HostCandidateHarvester(this.foundations));
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
			}
			added = false;
		}
		return added;
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
	
	public void harvest(IceMediaStream mediaStream, PortManager portManager, Selector selector) 
			throws HarvestException, NoCandidatesGatheredException {
		// Safe copy of currently registered harvesters
		Map<CandidateType, CandidateHarvester> copy;
		synchronized (this.harvesters) {
			copy = new HashMap<CandidateType, CandidateHarvester>(this.harvesters);
		}

		// Ask each harvester to gather candidates for the media stream
		// HOST candidates take precedence and are mandatory
		CandidateHarvester hostHarvester = copy.get(CandidateType.HOST);
		if(hostHarvester != null) {
			hostHarvester.harvest(portManager, mediaStream, selector);
		} else {
			throw new HarvestException("No HOST harvester registered!");
		}
		// Then comes the SRFLX, which depends on HOST candidates
		CandidateHarvester srflxHarvester = copy.get(CandidateType.SRFLX);
		if(srflxHarvester != null) {
			srflxHarvester.harvest(portManager, mediaStream, selector);
		}
		// RELAY candidates come last
		CandidateHarvester relayHarvester = copy.get(CandidateType.RELAY);
		if(relayHarvester != null) {
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
