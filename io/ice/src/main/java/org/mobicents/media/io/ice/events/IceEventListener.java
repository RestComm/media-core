package org.mobicents.media.io.ice.events;


/**
 * Listens for ICE-related events.
 * 
 * @author Henrique Rosa
 * 
 */
public interface IceEventListener {

	/**
	 * Event caught when all possible candidate pairs have been selected for an
	 * ICE Agent.
	 * 
	 * @param event
	 *            The event data
	 */
	void onSelectedCandidates(SelectedCandidatesEvent event);

}
