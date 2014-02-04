package org.mobicents.media.core.ice.harvest;

/**
 * Indicates that the harvesting completed without being able to bind any available
 * candidate to a NIO Channel.
 * 
 * @author Henrique Rosa
 * 
 */
public class NoCandidateBoundException extends HarvestingException {

	private static final long serialVersionUID = -3678692580311715002L;

	public NoCandidateBoundException() {
		super();
	}

	public NoCandidateBoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoCandidateBoundException(String message) {
		super(message);
	}

	public NoCandidateBoundException(Throwable cause) {
		super(cause);
	}
}
