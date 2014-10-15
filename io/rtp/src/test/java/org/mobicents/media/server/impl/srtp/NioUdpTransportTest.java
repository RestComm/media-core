package org.mobicents.media.server.impl.srtp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests transport layer of the DTLS handler
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class NioUdpTransportTest {

	private static DatagramChannel localChannel;
	private static DatagramChannel remoteChannel;

	@BeforeClass
	public static void beforeClass() {
		localChannel = openChannel();
		remoteChannel = openChannel();
		try {
			localChannel.connect(remoteChannel.getLocalAddress());
		} catch (Exception e) {
			closeChannel(localChannel);
			closeChannel(remoteChannel);
		}
	}

	@AfterClass
	public static void afterClass() {
		closeChannel(localChannel);
		closeChannel(remoteChannel);
	}

	private static DatagramChannel openChannel() {
		DatagramChannel channel = null;
		try {
			channel = DatagramChannel.open();
			channel.bind(new InetSocketAddress("127.0.0.1", 0));
			return channel;
		} catch (IOException e) {
			if (channel != null && channel.isOpen()) {
				try {
					channel.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return null;
		}
	}

	private static void closeChannel(DatagramChannel channel) {
		if (channel != null && channel.isOpen()) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Test
	public void testReceive() throws IOException {
		// given
		NioUdpTransport dtlsTransport = new NioUdpTransport(localChannel);
		String msg= "this is data";
		byte[] data = msg.getBytes();
		byte[] recvBuffer = new byte[data.length];
		ByteBuffer sendBuffer = ByteBuffer.allocate(data.length);
		sendBuffer.put(data);

		// when
		remoteChannel.send(sendBuffer, localChannel.getLocalAddress());
		dtlsTransport.receive(recvBuffer, 0, recvBuffer.length, 0);
		
		// then
		Assert.assertEquals(msg, new String(data));
	}

	@Test(expected = IllegalStateException.class)
	public void testTimeout() throws InterruptedException, IOException {
		// given
		NioUdpTransport dtlsTransport = new NioUdpTransport(localChannel);

		// when
		Thread.sleep(NioUdpTransport.MAX_DELAY);
		dtlsTransport.receive(new byte[5], 0, 5, 0);
	}

}
