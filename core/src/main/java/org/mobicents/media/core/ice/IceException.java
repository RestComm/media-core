package org.mobicents.media.core.ice;

/**
 * 
 * @author Henrique Rosa
 *
 */
public class IceException extends Exception {

	private static final long serialVersionUID = -4740400846650381855L;

	public IceException() {
		super();
	}

	public IceException(String message, Throwable cause) {
		super(message, cause);
	}

	public IceException(String message) {
		super(message);
	}

	public IceException(Throwable cause) {
		super(cause);
	}
}
