package org.mobicents.media.core.ice.events;

/**
 * Listens for ICE-related events.
 * 
 * @author Henrique Rosa
 * 
 */
public interface IceEventListener {

	void onSelectedCandidatePair(CandidatePairSelectedEvent event);

}
