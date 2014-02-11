package org.mobicents.media.core.ice.network;

import java.io.IOException;

public interface PacketTransformer<T extends Packet> {

	T decode(byte[] data) throws IOException;
	
	byte[] encode(T packet) throws IOException;
	
}
