package org.mobicents.media.io.ice.harvest;

import java.nio.channels.Selector;

import org.mobicents.media.io.ice.CandidateType;
import org.mobicents.media.io.ice.IceMediaStream;
import org.mobicents.media.server.io.network.PortManager;

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
	void harvest(PortManager portManager, IceMediaStream mediaStream, Selector selector) throws HarvestException;

	/**
	 * Gets the type of candidates gathered by the harvester.
	 * 
	 * @return The type of harvested candidates
	 */
	CandidateType getCandidateType();

}
