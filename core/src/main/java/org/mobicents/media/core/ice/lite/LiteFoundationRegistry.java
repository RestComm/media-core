package org.mobicents.media.core.ice.lite;

import org.mobicents.media.core.ice.FoundationRegistry;
import org.mobicents.media.core.ice.IceCandidate;

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
public class LiteFoundationRegistry extends FoundationRegistry {

	@Override
	public String computeIdentifier(IceCandidate candidate) {
		return candidate.getAddress().getHostAddress();
	}
}
