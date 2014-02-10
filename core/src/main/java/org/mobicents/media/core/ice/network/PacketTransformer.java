package org.mobicents.media.core.ice.network;

public interface PacketTransformer<T extends Packet> {

	T encode(byte[] data);
	
	byte[] decode(T packet);
	
}
