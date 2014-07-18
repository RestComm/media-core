package org.mobicents.media.server.io.network.handler;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline that selects a capable {@link ProtocolHandler} to process incoming
 * packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ProtocolHandlerPipeline {

	/**
	 * List of registered handlers in the pipeline.
	 */
	private final List<ProtocolHandler> handlers;
	
	public ProtocolHandlerPipeline() {
		this.handlers = new ArrayList<ProtocolHandler>(5);
	}
	
	/**
	 * Registers a new protocol handler in the pipeline.<br>
	 * Cannot register same handler twice.
	 * 
	 * @param handler
	 *            The handler to be registered.
	 * @return Whether the handler was successfully registered or not.
	 */
	public boolean addHandler(ProtocolHandler handler) {
		synchronized (this.handlers) {
			if(!handlers.contains(handler)) {
				handlers.add(handler);
				return true;
			}
			return false;
		}
	}
	
	/**
	 * Gets the protocol handler capable of processing the packet.
	 * 
	 * @param packet
	 *            The packet to be processed
	 * @return The protocol handler capable of processing the packet.<br>
	 *         Returns null in case no capable handler exists.
	 */
	public ProtocolHandler getHandler(byte[] packet) {
		// Copy the current handlers for thread safety
		List<ProtocolHandler> handlersCopy;
		synchronized (this.handlers) {
			handlersCopy = new ArrayList<ProtocolHandler>(this.handlers);
		}

		// Search for the first handler capable of processing the packet
		for (ProtocolHandler protocolHandler : handlersCopy) {
			if(protocolHandler.canHandle(packet)) {
				return protocolHandler;
			}
		}
		
		// Return null in case no handler is capable of decoding the packet
		return null;
	}

}
