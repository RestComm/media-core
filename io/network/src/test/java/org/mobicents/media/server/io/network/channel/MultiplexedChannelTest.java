package org.mobicents.media.server.io.network.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

import org.junit.Before;

/**
 * 
 * @author Henrique Rosa
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
		if(udpChannel != null) {
			if(udpChannel.isConnected()) {
				udpChannel.disconnect();
			}
			if(udpChannel.isOpen()) {
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
	

	public void testReceive() {
			// given
			MultiplexedChannel channel = new MultiplexedChannel();
			
			// when
			
			
			// then
			
	}
	
}
