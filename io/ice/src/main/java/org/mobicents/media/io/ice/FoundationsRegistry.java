package org.mobicents.media.io.ice;

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
