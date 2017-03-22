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

package org.restcomm.media.rtcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.network.deprecated.channel.MultiplexedChannel;
import org.restcomm.media.network.deprecated.channel.PacketHandlerException;
import org.restcomm.media.rtcp.RtcpBye;
import org.restcomm.media.rtcp.RtcpHandler;
import org.restcomm.media.rtcp.RtcpIntervalCalculator;
import org.restcomm.media.rtcp.RtcpPacket;
import org.restcomm.media.rtcp.RtcpPacketType;
import org.restcomm.media.rtcp.RtcpReceiverReport;
import org.restcomm.media.rtcp.RtcpReportBlock;
import org.restcomm.media.rtcp.RtcpSdes;
import org.restcomm.media.rtcp.RtcpSdesChunk;
import org.restcomm.media.rtcp.RtcpSdesItem;
import org.restcomm.media.rtcp.RtcpSenderReport;
import org.restcomm.media.rtp.CnameGenerator;
import org.restcomm.media.rtp.RtpClock;
import org.restcomm.media.rtp.SsrcGenerator;
import org.restcomm.media.rtp.statistics.RtpMember;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.scheduler.Scheduler;
import org.restcomm.media.scheduler.ServiceScheduler;
import org.restcomm.media.scheduler.WallClock;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandlerTest {
	
	private static final Logger logger = Logger.getLogger(RtcpHandlerTest.class);
	
	private static final byte[] RTCP_BYE_PACKET = new byte[] { 
		(byte) 0x81, (byte) 0xc8, 0x00, 0x0c, (byte) 0xf1, (byte) 0xcf, (byte) 0xb8, (byte) 0xf9, (byte) 0xd7, (byte) 0xc3, 0x17, (byte) 0xd1, (byte) 0xdd, (byte) 0xb2, 0x2d, 0x0e,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, (byte) 0xee, 0x7c, (byte) 0xb9, 0x07, (byte) 0xac, (byte) 0xbe,
		0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x83, 0x53, 0x00, 0x00, 0x00, 0x05, (byte) 0x99, 0x4f, 0x54, 0x18,
		0x00, 0x02, (byte) 0xb6, 0x04, (byte) 0x81, (byte) 0xca, 0x00, 0x06, (byte) 0xf1, (byte) 0xcf, (byte) 0xb8, (byte) 0xf9, 0x01, 0x10, 0x6d, 0x4e,
		0x56, 0x35, 0x51, 0x31, 0x36, 0x61, 0x6a, 0x52, 0x76, 0x4d, 0x30, 0x30, 0x77, 0x53, 0x00, 0x00,
		(byte) 0x81, (byte) 0xcb, 0x00, 0x01, (byte) 0xf1, (byte) 0xcf, (byte) 0xb8, (byte) 0xf9
		};
	
	private static final byte[] RTP_PACKET = new byte[] {
		(byte) 0x80, 0x00, 0x04, 0x5c, 0x00, 0x02, (byte) 0xb9, (byte) 0x80, (byte) 0xf1, (byte) 0xcf, (byte) 0xb8, (byte) 0xf9, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
		(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff
	};
	
	// Default messages
	private static final String INTERVAL_RANGE = "The interval (%d) must be in range [%d;%d]";
	
	private Clock wallClock;
	private RtpClock rtpClock;
	private RtpStatistics statistics;
	private RtcpHandler handler;
	private Scheduler scheduler;

	@Before
	public void before() {
		wallClock = new WallClock();
		scheduler = new ServiceScheduler();
		rtpClock = new RtpClock(wallClock);
		statistics = new RtpStatistics(rtpClock);
		handler = new RtcpHandler(scheduler, statistics);
		
		scheduler.start();
	}

	@After
	public void after() {
		if (handler.isJoined()) {
			handler.leaveRtpSession();
		}
		scheduler.stop();
	}

	@Test
	public void testCanHandle() {
		// given
		
		// when
		boolean canHandleRtcp = handler.canHandle(RTCP_BYE_PACKET);
		boolean canHandleRtp = handler.canHandle(RTP_PACKET);
		
		// then
		Assert.assertTrue(canHandleRtcp);
		Assert.assertFalse(canHandleRtp);
	}
	
	@Test
	public void testHandleReceiverReport() throws PacketHandlerException {
		// given
		InetSocketAddress localPeer = new InetSocketAddress("127.0.0.1", 6100);
		InetSocketAddress remotePeer = new InetSocketAddress("127.0.0.1", 6200);
		
		long remoteSsr = SsrcGenerator.generateSsrc();
		String remoteCname = CnameGenerator.generateCname();
		
		RtcpReceiverReport rr = new RtcpReceiverReport(false, remoteSsr);
		
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(remoteSsr);
		RtcpSdesItem sdesCname = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, remoteCname);
		sdesChunk.addRtcpSdesItem(sdesCname);
		sdes.addRtcpSdesChunk(sdesChunk);
		
		RtcpPacket rtcpPacket = new RtcpPacket(rr, sdes);
		byte[] rtcpData = new byte[300];
		rtcpPacket.encode(rtcpData, 0);
		
		// when
		handler.joinRtpSession();
		byte[] response = handler.handle(rtcpData, localPeer, remotePeer);

		// then
		// There are no responses in RTCP
		Assert.assertNull(response);
		
		/*
		 * When an RTCP packet is received from a participant whose SSRC is not in the
		 * member table, the SSRC is added to the table, and the value for
		 * members is updated once the participant has been validated.
		 */
		RtpMember newMember = statistics.getMember(remoteSsr);
		Assert.assertEquals(2, statistics.getMembers());
		Assert.assertNotNull(newMember);
		Assert.assertEquals(remoteSsr, newMember.getSsrc());
		Assert.assertEquals(remoteCname, newMember.getCname());
		
		// The average packet size is updated:
		// avg_rtcp_size = (1/16) * packet_size + (15/16) * avg_rtcp_size
		double expectedSize = (1.0 / 16.0) * rtcpPacket.getSize() + (15.0 / 16.0) * RtpStatistics.RTCP_DEFAULT_AVG_SIZE;
		Assert.assertEquals(expectedSize, statistics.getRtcpAvgSize(), 0.0);
	}

	@Test
	public void testHandleSenderReport() throws PacketHandlerException {
		// given
		InetSocketAddress localPeer = new InetSocketAddress("127.0.0.1", 6100);
		InetSocketAddress remotePeer = new InetSocketAddress("127.0.0.1", 6200);
		
		long localSsrc = statistics.getSsrc();
		long remoteSsrc = SsrcGenerator.generateSsrc();
		String remoteCname = CnameGenerator.generateCname();
		
		TimeStamp ntp = new TimeStamp(wallClock.getCurrentTime());
		RtcpSenderReport sr = new RtcpSenderReport(false, remoteSsrc, ntp.getSeconds(), ntp.getFraction(), 200, 3, 3 * 200);
		RtcpReportBlock rrBlock = new RtcpReportBlock(localSsrc, 1, 1, 0, 10, 0, 398416412, 223412);
		sr.addReceiverReport(rrBlock);
		
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(remoteSsrc);
		RtcpSdesItem sdesCname = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, remoteCname);
		sdesChunk.addRtcpSdesItem(sdesCname);
		sdes.addRtcpSdesChunk(sdesChunk);
		
		RtcpPacket rtcpPacket = new RtcpPacket(sr, sdes);
		byte[] rtcpData = new byte[300];
		rtcpPacket.encode(rtcpData, 0);
		
		// when
		handler.joinRtpSession();
		byte[] response = handler.handle(rtcpData, localPeer, remotePeer);
		
		// then
		// There are no responses in RTCP
		Assert.assertNull(response);
		
		/*
		 * When an RTCP packet is received from a participant whose SSRC is not in the
		 * member table, the SSRC is added to the table, and the value for
		 * members is updated once the participant has been validated.
		 */
		RtpMember newMember = statistics.getMember(remoteSsrc);
		Assert.assertEquals(2, statistics.getMembers());
		Assert.assertNotNull(newMember);
		Assert.assertEquals(remoteSsrc, newMember.getSsrc());
		Assert.assertEquals(remoteCname, newMember.getCname());
		
		Assert.assertEquals(sr.getNtpTs(), newMember.getLastSR());
		Assert.assertEquals(0, newMember.getReceivedSinceSR());
		
		// The average packet size is updated:
		// avg_rtcp_size = (1/16) * packet_size + (15/16) * avg_rtcp_size
		double expectedSize = (1.0/16.0) * rtcpPacket.getSize() + (15.0/16.0) * RtpStatistics.RTCP_DEFAULT_AVG_SIZE;
		Assert.assertEquals(expectedSize, statistics.getRtcpAvgSize(), 0.0);
	}
	
	@Test
	public void testHandleBye() throws PacketHandlerException {
		// given
		InetSocketAddress localPeer = new InetSocketAddress("127.0.0.1", 6100);
		InetSocketAddress remotePeer = new InetSocketAddress("127.0.0.1", 6200);
		
		RtcpPacket rr1 = buildReceiverReportPacket();
		byte[] rr1Data = new byte[300];
		rr1.encode(rr1Data, 0);

		RtcpPacket rr2 = buildReceiverReportPacket();
		byte[] rr2Data = new byte[300];
		rr2.encode(rr2Data, 0);
		
		RtcpPacket rr3 = buildReceiverReportPacket();
		byte[] rr3Data = new byte[300];
		rr3.encode(rr3Data, 0);
		
		RtcpPacket bye = buildReceiverReportPacket(rr1.getReceiverReport().ssrc, rr1.getSdes().getCname(), true);
		byte[] byeData = new byte[300];
		bye.encode(byeData, 0);
		
		// when
		handler.joinRtpSession();
		handler.handle(rr1Data, localPeer, remotePeer);
		double expectedSize = (1.0/16.0) * rr1.getSize() + (15.0/16.0) * RtpStatistics.RTCP_DEFAULT_AVG_SIZE;

		handler.handle(rr2Data, localPeer, remotePeer);
		expectedSize = (1.0/16.0) * rr2.getSize() + (15.0/16.0) * expectedSize;
		
		handler.handle(rr3Data, localPeer, remotePeer);
		expectedSize = (1.0/16.0) * rr3.getSize() + (15.0/16.0) * expectedSize;
		
		// then
		long expectedMembers = 4;
		Assert.assertEquals(expectedMembers, statistics.getMembers());
		
		// when
		handler.handle(byeData, localPeer, remotePeer);
		expectedSize = (1.0/16.0) * bye.getSize() + (15.0/16.0) * expectedSize;
		
		// then
		/*
		 * if the received packet is an RTCP BYE packet, the SSRC is checked
		 * against the member table. If present, the entry is removed from the
		 * table, and the value for members is updated. The SSRC is then checked
		 * against the sender table. If present, the entry is removed from the
		 * table, and the value for senders is updated.
		 */
		Assert.assertEquals(expectedMembers - 1, statistics.getMembers());
		Assert.assertEquals(expectedSize, statistics.getRtcpAvgSize(), 0.0);
	}
	
	private RtcpPacket buildReceiverReportPacket() {
		long ssrc = SsrcGenerator.generateSsrc();
		String cname = CnameGenerator.generateCname();
		
		RtcpReceiverReport rr = new RtcpReceiverReport(false, ssrc);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(ssrc);
		RtcpSdesItem sdesCname = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, cname);
		sdesChunk.addRtcpSdesItem(sdesCname);
		sdes.addRtcpSdesChunk(sdesChunk);
		return new RtcpPacket(rr, sdes);
	}

	private RtcpPacket buildReceiverReportPacket(long ssrc, String cname, boolean bye) {
		RtcpReceiverReport rr = new RtcpReceiverReport(false, ssrc);
		RtcpSdes sdes = new RtcpSdes(false);
		RtcpSdesChunk sdesChunk = new RtcpSdesChunk(ssrc);
		RtcpSdesItem sdesCname = new RtcpSdesItem(RtcpSdesItem.RTCP_SDES_CNAME, cname);
		sdesChunk.addRtcpSdesItem(sdesCname);
		sdes.addRtcpSdesChunk(sdesChunk);
		
		RtcpBye rtcpBye = null;
		if(bye) {
			rtcpBye = new RtcpBye(false);
			return new RtcpPacket(rr, sdes, rtcpBye);
		}
		return new RtcpPacket(rr, sdes);
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
		Assert.assertTrue(recvChannel.rxPackets > 0);
		Assert.assertTrue(recvChannel.rxOctets > 0);
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
