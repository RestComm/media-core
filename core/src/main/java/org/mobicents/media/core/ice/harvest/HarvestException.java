package org.mobicents.media.core.ice.harvest;

import org.mobicents.media.core.ice.IceException;

/**
 * Exception that may occur while harvesting candidates.
 * 
 * @author Henrique Rosa
 * 
 */
public class HarvestException extends IceException {

	private static final long serialVersionUID = 4361875988093843175L;

	public HarvestException() {
		super();
	}

	public HarvestException(String message, Throwable cause) {
		super(message, cause);
	}

	public HarvestException(String message) {
		super(message);
	}

	public HarvestException(Throwable cause) {
		super(cause);
	}
}
