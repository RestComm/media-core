package org.mobicents.media.server.io.network.channel;

/**
 * Exception that is usually thrown when a {@link PacketHandler} cannot handle
 * an incoming packet.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketHandlerException extends Exception {

	private static final long serialVersionUID = -7774399750471780984L;

	public PacketHandlerException() {
		super();
	}

	public PacketHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public PacketHandlerException(String message) {
		super(message);
	}

	public PacketHandlerException(Throwable cause) {
		super(cause);
	}
	
}
