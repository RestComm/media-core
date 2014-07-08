package org.mobicents.media.server.io.network.handler;

/**
 * Exception that is usually thrown when a {@link ProtocolHandler} cannot handle
 * an incoming packet.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ProtocolHandlerException extends Exception {

	private static final long serialVersionUID = -7774399750471780984L;

	public ProtocolHandlerException() {
		super();
	}

	public ProtocolHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ProtocolHandlerException(String message) {
		super(message);
	}

	public ProtocolHandlerException(Throwable cause) {
		super(cause);
	}
	
}
