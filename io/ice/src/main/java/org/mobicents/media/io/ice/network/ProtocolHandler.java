package org.mobicents.media.io.ice.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * 
 * @author Henrique Rosa
 *
 */
public interface ProtocolHandler {

	String getProtocol();
	
	byte[] process(SelectionKey key, byte[] data, int length) throws IOException;

}
