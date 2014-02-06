package org.mobicents.media.core.ice.harvest;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.media.core.ice.CandidateType;
import org.mobicents.media.core.ice.FoundationsRegistry;
import org.mobicents.media.core.ice.HostCandidateHarvester;
import org.mobicents.media.core.ice.IceMediaStream;
import org.mobicents.media.core.ice.lite.LiteFoundationsRegistry;

/**
 * Manages the candidate harvesting process
 * 
 * @author Henrique Rosa
 * 
 */
public class HarvestManager {

	private final FoundationsRegistry foundations;
	private final List<CandidateHarvester> harvesters;

	public HarvestManager() {
		this.foundations = new LiteFoundationsRegistry();
		this.harvesters = new ArrayList<CandidateHarvester>(
				CandidateType.count());
		this.harvesters.add(new HostCandidateHarvester(this.foundations));
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

	public void harvest(IceMediaStream mediaStream, int preferredPort)
			throws HarvestException, NoCandidatesGatheredException {
		List<CandidateHarvester> copy;
		synchronized (this.harvesters) {
			copy = new ArrayList<CandidateHarvester>(this.harvesters);
		}

		// Ask each harvester to gather candidates for the media stream
		for (CandidateHarvester harvester : copy) {
			harvester.harvest(preferredPort, mediaStream);
		}

		// After harvesting all possible candidates, ask the media stream to
		// select its default local candidates
		mediaStream.getRtpComponent().selectDefaultLocalCandidate();
		if (mediaStream.supportsRtcp()) {
			mediaStream.getRtcpComponent().selectDefaultLocalCandidate();
		}

		if (!mediaStream.hasLocalRtpCandidates()) {
			throw new NoCandidatesGatheredException(
					"No RTP candidates were gathered for "
							+ mediaStream.getName() + " stream");
		}
	}

}
