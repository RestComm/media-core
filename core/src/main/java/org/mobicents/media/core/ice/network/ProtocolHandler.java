package org.mobicents.media.core.ice.network;

import java.io.IOException;
import java.nio.channels.SelectionKey;

/**
 * 
 * @author Henrique Rosa
 *
 */
public interface ProtocolHandler {

	String getProtocol();
	
	byte[] handleRead(SelectionKey key, byte[] data, int length) throws IOException;

	byte[] handleWrite(SelectionKey key, byte[] data, int length) throws IOException;

}
