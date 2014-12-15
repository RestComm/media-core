package org.mobicents.media.io.ice.harvest;

/**
 * Indicates that the harvesting completed without being able to bind any available
 * candidate to a NIO Channel.
 * 
 * @author Henrique Rosa
 * 
 */
public class NoCandidatesGatheredException extends HarvestException {

	private static final long serialVersionUID = -3678692580311715002L;

	public NoCandidatesGatheredException() {
		super();
	}

	public NoCandidatesGatheredException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoCandidatesGatheredException(String message) {
		super(message);
	}

	public NoCandidatesGatheredException(Throwable cause) {
		super(cause);
	}
}
