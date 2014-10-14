package org.mobicents.media.server.io.network.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Timer;
import java.util.TimerTask;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link MultiplexedChannel}
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannelTest {

	private DatagramChannel udpChannel;

	@Before
	public void before() throws IOException {
		this.udpChannel = DatagramChannel.open();
		udpChannel.bind(new InetSocketAddress("127.0.0.1", 0));
	}

	@After
	public void after() throws IOException {
		if (udpChannel != null) {
			if (udpChannel.isConnected()) {
				udpChannel.disconnect();
			}
			if (udpChannel.isOpen()) {
				udpChannel.close();
			}
		}
	}

	@Test
	public void testQueueEmptyData() {
		// given
		MultiplexedChannel channel = new MultiplexedChannel();

		// when
		channel.queueData(null);
		channel.queueData(new byte[0]);

		// then
		assertFalse(channel.hasPendingData());
	}

	@Test
	public void testQueueData() {
		// given
		MultiplexedChannel channel = new MultiplexedChannel();

		// when
		channel.queueData("hello".getBytes());

		// then
		assertTrue(channel.hasPendingData());
	}

	@Test
	public void testChannelSetup() throws IOException {
		// given
		MultiplexedChannel channel = new MultiplexedChannel();
		String localAddress = ((InetSocketAddress) this.udpChannel.getLocalAddress()).getHostString();
		int port = this.udpChannel.socket().getLocalPort();

		// when
		channel.setChannel(this.udpChannel);

		// then
		Assert.assertTrue(channel.isOpen());
		Assert.assertFalse(channel.isConnected());
		Assert.assertEquals(localAddress, channel.getLocalAddress());
		Assert.assertEquals(port, channel.getLocalPort());
	}

	@Test
	public void testReceive() throws IOException, InterruptedException {
		// given
		MultiplexedChannel channel = new MultiplexedChannel();
		PacketHandlerMock handler = new LowPriorityPacketHandlerMock();
		MockClient client = new MockClient();

		// when
		channel.handlers.addHandler(handler);
		channel.setChannel(this.udpChannel);
		client.setRemotePeer(this.udpChannel.getLocalAddress());
		client.start();
		
		for(int i=0; i < 5; i++) {
			Thread.sleep(20);
			channel.receive();
		}
		
		// then
		Assert.assertTrue(channel.hasPendingData());
	}
	
	/**
	 * Basic client that sends packets to the multiplexed channel
	 * @author Henrique Rosa
	 *
	 */
	private class MockClient {
		
		public static final int MAX_SEND = 5;
		
		private DatagramChannel clientChannel;
		private SocketAddress remotePeer;
		private final Timer timer;
		private final SendTask sendTask;
		private final ByteBuffer buffer;
		private int sentPackets;
		private boolean running;
		
		public MockClient() {
			this.timer = new Timer();
			this.sendTask = new SendTask();
			this.buffer = ByteBuffer.allocate(50);
			this.buffer.put(LowPriorityPacketHandlerMock.DATA.getBytes());
			this.buffer.flip();
			this.sentPackets = 0;
			this.running = false;
		}
		
		public void setRemotePeer(SocketAddress remotePeer) {
			this.remotePeer = remotePeer;
		}
		
		public void start() throws IOException {
			if(!this.running && this.remotePeer != null) {
				this.clientChannel = DatagramChannel.open();
				this.clientChannel.bind(new InetSocketAddress("127.0.0.1", 0));
				this.running = true;
				this.timer.schedule(sendTask, 1, 20);
			}
		}
		
		public void stop() {
			if(this.running) {
				this.running = false;
				try {
					this.clientChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.timer.cancel();
			}
		}
		
		void onPacketSent() {
			this.sentPackets++;
			if(this.sentPackets == MAX_SEND) {
				stop();
			}
		}
		
		private class SendTask extends TimerTask {

			@Override
			public void run() {
				if(running && clientChannel.isOpen()) {
					try {
						clientChannel.send(buffer, remotePeer);
					} catch (IOException e) {
						stop();
					}
					onPacketSent();
				}
			}
			
		}
		
		
	}

}
