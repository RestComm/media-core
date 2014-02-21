package org.mobicents.media.server.impl.srtp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 * Datagram Transport implementation that uses NIO instead of bocking IO.
 * 
 * @author Henrique Rosa
 * 
 */
public class NioUdpTransport implements DatagramTransport {

	private final static int MIN_IP_OVERHEAD = 20;
	private final static int MAX_IP_OVERHEAD = MIN_IP_OVERHEAD + 64;
	private final static int UDP_OVERHEAD = 8;

	private final DatagramChannel channel;
	private final ByteBuffer readBuffer;
	private final ByteBuffer writeBuffer;
	private final int receiveLimit;
	private final int sendLimit;

	public NioUdpTransport(DatagramChannel channel, int mtu) {
		if (!channel.isConnected()) {
			throw new IllegalArgumentException(
					"The datagram channel must be connected");
		}
		this.channel = channel;

		// NOTE: As of JDK 1.6, can use NetworkInterface.getMTU
		this.receiveLimit = mtu - MIN_IP_OVERHEAD - UDP_OVERHEAD;
		this.sendLimit = mtu - MAX_IP_OVERHEAD - UDP_OVERHEAD;

		this.readBuffer = ByteBuffer.allocate(this.receiveLimit);
		this.writeBuffer = ByteBuffer.allocate(this.sendLimit);
	}

	public int getReceiveLimit() throws IOException {
		return this.receiveLimit;
	}

	public int getSendLimit() throws IOException {
		return this.sendLimit;
	}

	public int receive(byte[] buf, int off, int len, int waitMillis)
			throws IOException {
		this.readBuffer.clear();
		this.readBuffer.get(buf, off, len);
		this.channel.receive(readBuffer);
		return this.readBuffer.position();
	}

	public void send(byte[] buf, int off, int len) throws IOException {
		this.writeBuffer.clear();
		this.writeBuffer.get(buf, off, len);
		this.channel.send(this.writeBuffer, this.channel.getRemoteAddress());
	}

	public void close() throws IOException {
		if (this.channel.isOpen()) {
			this.readBuffer.clear();
			this.writeBuffer.clear();
			this.channel.close();
		}
	}

}
