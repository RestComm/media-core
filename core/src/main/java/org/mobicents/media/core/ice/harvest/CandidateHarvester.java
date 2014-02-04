package org.mobicents.media.core.ice.harvest;

import java.util.List;

import org.mobicents.media.core.ice.FoundationsRegistry;
import org.mobicents.media.core.ice.candidate.LocalCandidateWrapper;

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
	 * @param port
	 *            the port used to bind the candidates to.
	 * @param foundationsRegistry
	 *            the registry that relates the harvested addresses with the
	 *            foundation value
	 * @return The list of harvested candidates
	 * @throws HarvestingException
	 *             When an error occurs during the candidate harvesting process.
	 * @throws NoCandidateBoundException
	 *             When the harvesting process finishes without finding any
	 *             available candidate
	 */
	List<LocalCandidateWrapper> harvest(int port,
			FoundationsRegistry foundationsRegistry)
			throws HarvestingException, NoCandidateBoundException;

}
