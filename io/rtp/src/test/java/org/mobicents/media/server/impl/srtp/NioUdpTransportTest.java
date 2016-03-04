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

package org.mobicents.media.server.impl.srtp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
			channel.configureBlocking(false);
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
	
	@Ignore
	@Test
	public void testReceive() throws IOException {
		// given
		String msg= "this is a really small chunk of data";
		byte[] data = msg.getBytes();
		byte[] recvBuffer = new byte[data.length];
		ByteBuffer sendBuffer = ByteBuffer.allocate(data.length);
		sendBuffer.put(data);

		// when
		NioUdpTransport dtlsTransport = new NioUdpTransport(localChannel);
		remoteChannel.send(sendBuffer, localChannel.getLocalAddress());
		dtlsTransport.receive(recvBuffer, 0, recvBuffer.length, 0);
		
		// then
		Assert.assertEquals(msg, new String(data));
		Assert.assertEquals(msg, new String(recvBuffer));
	}

	@Test(timeout = NioUdpTransport.MAX_DELAY * 2, expected = IllegalStateException.class)
	public void testTimeout() throws InterruptedException, IOException {
		// given
		NioUdpTransport dtlsTransport = new NioUdpTransport(localChannel);

		// when
		Thread.sleep(NioUdpTransport.MAX_DELAY + 1000);
		dtlsTransport.receive(new byte[5], 0, 5, 0);
	}

}
