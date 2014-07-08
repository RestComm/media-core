package org.mobicents.media.server.io.network.handler;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * Represents an endpoint capable of multiplexing incoming traffic.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface Multiplexer {
	
	void receive(SelectionKey key) throws IOException;
	
	void send(SelectionKey key) throws IOException;
	
	void close();

}
