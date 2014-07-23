package org.mobicents.media.server.io.network.handler;

import java.io.IOException;

/**
 * Represents an endpoint capable of multiplexing incoming traffic.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public interface Multiplexer {
	
	void receive() throws IOException;
	
	void send() throws IOException;
	
	boolean isConnected();
	
	void disconnect() throws IOException;
	
	void close();

}
