package org.mobicents.media.server.io.sdp.exception;

/**
 * High level exception for SDP-related processing.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SdpException extends Exception {

	private static final long serialVersionUID = -775641181266307996L;

	public SdpException(String message, Throwable cause) {
		super(message, cause);
	}

	public SdpException(String message) {
		super(message);
	}

	public SdpException(Throwable cause) {
		super(cause);
	}

}
