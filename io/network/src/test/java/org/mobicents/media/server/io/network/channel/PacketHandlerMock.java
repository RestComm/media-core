package org.mobicents.media.server.io.network.channel;

import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * A mock of the Packet Handler.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public abstract class PacketHandlerMock implements PacketHandler {

	private int priority;
	private String validDataString;
	private byte[] validData;
	
	public PacketHandlerMock(int priority, String validData) {
		this.priority = priority;
		this.validDataString = validData;
		this.validData = this.validDataString.getBytes();
	}
	
	@Override
	public int compareTo(PacketHandler o) {
		if(o == null) {
			return 1;
		}
		return this.priority - o.getPipelinePriority();
	}

	@Override
	public boolean canHandle(byte[] packet) {
		return canHandle(packet, packet.length, 0);
	}

	@Override
	public boolean canHandle(byte[] packet, int dataLength, int offset) {
		byte[] dest = Arrays.copyOfRange(packet, offset, dataLength);
		return Arrays.equals(this.validData, dest);
	}

	@Override
	public byte[] handle(byte[] packet, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		return handle(packet, packet.length, 0, localPeer, remotePeer);
	}

	@Override
	public byte[] handle(byte[] packet, int dataLength, int offset, InetSocketAddress localPeer, InetSocketAddress remotePeer) throws PacketHandlerException {
		String request = new String(Arrays.copyOfRange(packet, offset, dataLength));
		return ("received " + request).getBytes();
	}

	@Override
	public int getPipelinePriority() {
		return this.priority;
	}

}
