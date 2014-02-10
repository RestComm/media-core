package org.mobicents.media.core.ice.network.stun;

import java.nio.channels.SelectionKey;

import org.mobicents.media.core.ice.network.PacketTransformer;

public class StunHandler implements PacketTransformer<StunPacket> {

	private SelectionKey selectionKey;
	
	public StunHandler(SelectionKey key) {
		this.selectionKey = key;
	}

	public StunPacket encode(byte[] data) {
		// TODO Auto-generated method stub
		return null;
	}

	public byte[] decode(StunPacket packet) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
