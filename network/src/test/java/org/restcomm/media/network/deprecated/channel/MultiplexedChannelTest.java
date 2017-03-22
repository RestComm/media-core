/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.network.deprecated.channel;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.media.network.deprecated.channel.MultiplexedChannel;

/**
 * Tests for {@link MultiplexedChannel}
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class MultiplexedChannelTest {

	private static DatagramChannel localChannel;
	private static DatagramChannel remoteChannel;

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
		String localAddress = ((InetSocketAddress) localChannel.getLocalAddress()).getHostString();
		int port = localChannel.socket().getLocalPort();

		// when
		channel.setTransport(localChannel);

		// then
		Assert.assertTrue(channel.isOpen());
		Assert.assertEquals(localChannel.isConnected(), channel.isConnected());
		Assert.assertEquals(localAddress, channel.getLocalHost());
		Assert.assertEquals(port, channel.getLocalPort());
	}

	@Test
	public void testSendReceive() throws IOException, InterruptedException {
		// given
		MultiplexedChannel channel = new MultiplexedChannel();
		PacketHandlerMock handler = new LowPriorityPacketHandlerMock();
		channel.handlers.addHandler(handler);
		channel.setTransport(localChannel);

		String msg = LowPriorityPacketHandlerMock.DATA;
		byte[] data = msg.getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(30);
		
		/*
		 *  RECEIVE
		 */
		// when
		buffer.put(data);
		buffer.flip();
		int sent = remoteChannel.send(buffer, localChannel.getLocalAddress());
		channel.receive();
		
		// then
		Assert.assertEquals(data.length, sent);
		Assert.assertTrue(channel.hasPendingData());
		
		/*
		 *  SEND
		 */
		// when
		channel.send();
		buffer.clear();
		SocketAddress received = remoteChannel.receive(buffer);
		buffer.flip();
		byte[] sentData = new byte[buffer.limit()];
		buffer.get(sentData, buffer.position(), buffer.limit());
		
		// then
		Assert.assertFalse(channel.hasPendingData());
		Assert.assertEquals(localChannel.getLocalAddress(), received);
		Assert.assertEquals("received "+msg, new String(sentData));
	}
	
	/*
	 * Test Setup
	 */

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
	
}
