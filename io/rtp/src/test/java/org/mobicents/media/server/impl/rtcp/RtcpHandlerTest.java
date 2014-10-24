package org.mobicents.media.server.impl.rtcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.io.network.channel.MultiplexedChannel;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandlerTest {
	
	private static final Logger logger = Logger.getLogger(RtcpHandlerTest.class);
	
	// Default messages
	private static final String INTERVAL_RANGE = "The interval (%d) must be in range [%d;%d]";
	
	private Clock wallClock;
	private RtpClock rtpClock;
	private RtpStatistics statistics;
	private RtcpHandler handler;

	@Before
	public void before() {
		wallClock = new DefaultClock();
		rtpClock = new RtpClock(wallClock);
		statistics = new RtpStatistics(rtpClock);
		handler = new RtcpHandler(statistics);
	}

	@After
	public void after() {
		if (handler.isJoined()) {
			handler.leaveRtpSession();
		}
	}

	@Test
	public void testJoinAndLeaveRtpSession() throws InterruptedException {
		// given
		// senders > members * 0.25 AND we_sent == false, then C = avg_rtcp_size / rtcp_bw
		double c = statistics.getRtcpAvgSize() / RtpStatistics.RTCP_DEFAULT_BW;
		// senders > members * 0.25 THEN n = members
		int n = statistics.getMembers();
		// initial == true, then Tmin = 2.5 seconds
		double tMin = RtcpIntervalCalculator.INITIAL_MIN_TIME;
		double tD = Math.max(tMin, n * c);
		// T = [Td * 0.5; Td * 1.5]
		// t = T / (e - 3/2)
		double compensation = Math.E - (3.0 / 2.0);
		long lowT = (long) (((tD * 0.5) / compensation) * 1000);
		long highT = (long) (((tD * 1.5) / compensation) * 1000);

		// when
		handler.joinRtpSession();
		long nextReport = handler.getNextScheduledReport();
		
		// give time to handler supposedly send the initial report
		Thread.sleep(handler.getNextScheduledReport());

		// then
		Assert.assertTrue(handler.isJoined());
		String msg = String.format(INTERVAL_RANGE, nextReport, lowT, highT);
		Assert.assertTrue(msg, nextReport >= lowT);
		Assert.assertTrue(msg, nextReport <= highT);
		Assert.assertEquals(RtcpPacketType.RTCP_REPORT, statistics.getRtcpPacketType());
		
		// Notice handler will still be in initial stage since it cannot send packets
		// (we have not set a proper data channel)
		Assert.assertTrue(handler.isInitial());
		
		// when
		/*
		 * When the participant decides to leave the system, tp is reset to tc,
		 * the current time, members and pmembers are initialized to 1, initial
		 * is set to 1, we_sent is set to false, senders is set to 0, and
		 * avg_rtcp_size is set to the size of the compound BYE packet.
		 * 
		 * The calculated interval T is computed. The BYE packet is then
		 * scheduled for time tn = tc + T.
		 */
		handler.leaveRtpSession();
		nextReport = handler.getNextScheduledReport();
		
		// then
		Assert.assertFalse(handler.isJoined());
		Assert.assertEquals(RtcpPacketType.RTCP_BYE, statistics.getRtcpPacketType());
	}
	
	@Test
	public void testRtcpSend() throws IOException, InterruptedException {
		/* GIVEN */
		SnifferChannel recvChannel = new SnifferChannel();
		recvChannel.open();
		recvChannel.bind(new InetSocketAddress("127.0.0.1", 0));
		
		DatagramChannel sendChannel = DatagramChannel.open();
		sendChannel.bind(new InetSocketAddress("127.0.0.1", 0));
		
		recvChannel.connect(sendChannel.getLocalAddress());
		sendChannel.connect(recvChannel.getLocalAddress());
		
		/* WHEN */
		handler.setChannel(sendChannel);
		handler.joinRtpSession();
		
		recvChannel.start();
		new Thread(recvChannel).start();
		Thread.sleep(15000);
		
		handler.leaveRtpSession();
		Thread.sleep(5000);
		recvChannel.stop();
		
		/* THEN */
		Assert.assertEquals(recvChannel.rxPackets, statistics.getRtcpPacketsSent());
		Assert.assertEquals(recvChannel.rxOctets, statistics.getRtcpOctetsSent());
	}
	
	
	private class SnifferChannel extends MultiplexedChannel implements Runnable {
		
		private volatile boolean running = false;
		private int rxPackets = 0;
		private int rxOctets = 0;
		
		private final ByteBuffer buffer = ByteBuffer.allocate(300);
		
		@Override
		public void receive() throws IOException {
			this.buffer.clear();
			int read = super.dataChannel.read(buffer);
			
			if(read > 0) {
				this.rxPackets++;
				this.rxOctets += read;
			}
		}
		
		public void start() {
			this.running = true;
		}
		
		public void stop() {
			this.running = false;
		}
		
		@Override
		public void run() {
			while(running) {
				try {
					receive();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					running = false;
				}
			}
		}
		
	}
	
}
