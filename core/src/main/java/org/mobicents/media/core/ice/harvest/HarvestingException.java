package org.mobicents.media.core.ice.harvest;

/**
 * Exception that may occur while harvesting candidates.
 * 
 * @author Henrique Rosa
 * 
 */
public class HarvestingException extends Exception {

	private static final long serialVersionUID = 4361875988093843175L;

	public HarvestingException() {
		super();
	}

	public HarvestingException(String message, Throwable cause) {
		super(message, cause);
	}

	public HarvestingException(String message) {
		super(message);
	}

	public HarvestingException(Throwable cause) {
		super(cause);
	}
}
