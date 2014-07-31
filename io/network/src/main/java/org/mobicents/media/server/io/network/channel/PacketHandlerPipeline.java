package org.mobicents.media.server.io.network.channel;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline that selects a capable {@link PacketHandler} to process incoming
 * packets.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class PacketHandlerPipeline {

	/**
	 * List of registered handlers in the pipeline.
	 */
	private final List<PacketHandler> handlers;
	
	public PacketHandlerPipeline() {
		this.handlers = new ArrayList<PacketHandler>(5);
	}
	
	/**
	 * Registers a new protocol handler in the pipeline.<br>
	 * Cannot register same handler twice.
	 * 
	 * @param handler
	 *            The handler to be registered.
	 * @return Whether the handler was successfully registered or not.
	 */
	public boolean addHandler(PacketHandler handler) {
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
	public PacketHandler getHandler(byte[] packet) {
		// Copy the current handlers for thread safety
		List<PacketHandler> handlersCopy;
		synchronized (this.handlers) {
			handlersCopy = new ArrayList<PacketHandler>(this.handlers);
		}

		// Search for the first handler capable of processing the packet
		for (PacketHandler protocolHandler : handlersCopy) {
			if(protocolHandler.canHandle(packet)) {
				return protocolHandler;
			}
		}
		
		// Return null in case no handler is capable of decoding the packet
		return null;
	}

}
