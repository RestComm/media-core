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
	private final int receiveLimit;
	private final int sendLimit;

	public NioUdpTransport(DatagramChannel channel, int mtu) {
		if (!channel.isConnected()) {
			throw new IllegalArgumentException(
					"The datagram channel must be connected");
		}
		this.channel = channel;

		// TODO As of JDK 1.6, can use NetworkInterface.getMTU
		this.receiveLimit = mtu - MIN_IP_OVERHEAD - UDP_OVERHEAD;
		this.sendLimit = mtu - MAX_IP_OVERHEAD - UDP_OVERHEAD;
	}

	public int getReceiveLimit() throws IOException {
		return this.receiveLimit;
	}

	public int getSendLimit() throws IOException {
		return this.sendLimit;
	}

	public int receive(byte[] buf, int off, int len, int waitMillis)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
		return this.channel.read(buffer);
	}

	public void send(byte[] buf, int off, int len) throws IOException {
		if (len > getSendLimit()) {
			/*
			 * RFC 4347 4.1.1. "If the application attempts to send a record
			 * larger than the MTU, the DTLS implementation SHOULD generate an
			 * error, thus avoiding sending a packet which will be fragmented."
			 */
			// TODO Exception
		}
		ByteBuffer buffer = ByteBuffer.wrap(buf, off, len);
		this.channel.send(buffer, this.channel.getRemoteAddress());
	}

	public void close() throws IOException {
		if (this.channel.isOpen()) {
			this.channel.close();
		}
	}

}
