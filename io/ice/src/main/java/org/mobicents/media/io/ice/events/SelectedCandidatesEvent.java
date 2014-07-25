package org.mobicents.media.io.ice.events;

import org.mobicents.media.io.ice.IceAgent;

/**
 * Event that must be fired when an ICE Agent finishes selecting all candidate
 * pairs.
 * 
 * @author Henrique Rosa
 * 
 */
public class SelectedCandidatesEvent {

	private final IceAgent source;

	public SelectedCandidatesEvent(IceAgent source) {
		this.source = source;
	}

	public IceAgent getSource() {
		return source;
	}

}
