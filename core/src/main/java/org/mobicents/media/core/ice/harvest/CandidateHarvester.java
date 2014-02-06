package org.mobicents.media.core.ice.harvest;

import org.mobicents.media.core.ice.IceMediaStream;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public interface CandidateHarvester {

	int MIN_PORT = 1024;
	int MAX_PORT = 65535;

	/**
	 * Harvests candidates.
	 * 
	 * @param preferredPort
	 *            The preferred port to bind the candidates to.
	 * @param mediaStream
	 *            The media stream to bind the candidate to.
	 * @throws HarvestException
	 *             When an error occurs during the candidate harvesting process.
	 */
	void harvest(int preferredPort, IceMediaStream mediaStream)
			throws HarvestException;

}
