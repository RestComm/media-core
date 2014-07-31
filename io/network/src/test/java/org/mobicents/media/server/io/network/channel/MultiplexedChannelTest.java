package org.mobicents.media.server.io.network.channel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

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

	public void testReceive() {
			// given
			MultiplexedChannel channel = new MultiplexedChannel();
			
			// when
			
			
			// then
			
	}
	
}
