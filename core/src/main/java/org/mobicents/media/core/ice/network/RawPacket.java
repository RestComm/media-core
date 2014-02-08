package org.mobicents.media.core.ice.network;

import org.ice4j.TransportAddress;

/**
 * Represent a raw binary packet to be transmitted between two peers.
 * 
 * @author Henrique Rosa
 * 
 */
public class RawPacket {

	private byte[] data;
	private int dataLength;
	private TransportAddress localAddress;
	private TransportAddress remoteAddress;

	public RawPacket(byte[] data, int dataLength,
			TransportAddress localAddress, TransportAddress remoteAddress) {
		this.data = new byte[dataLength];
		System.arraycopy(data, 0, this.data, 0, dataLength);
		this.dataLength = dataLength;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
	}

	/**
	 * Gets the binary representation of the packet
	 * 
	 * @return the array of bytes that compose the packet
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Get the length of the packet
	 * 
	 * @return
	 */
	public int getDataLength() {
		return dataLength;
	}

	/**
	 * Gets the address from where the packet came from
	 * 
	 * @return The local address of the packet.
	 */
	public TransportAddress getLocalAddress() {
		return localAddress;
	}

	/**
	 * Gets the address to where the packet must be sent.
	 * 
	 * @return The remote address of the packet
	 */
	public TransportAddress getRemoteAddress() {
		return remoteAddress;
	}

}
