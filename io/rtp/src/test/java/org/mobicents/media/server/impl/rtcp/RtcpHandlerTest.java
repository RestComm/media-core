package org.mobicents.media.server.impl.rtcp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.WallTestClock;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.scheduler.Clock;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpHandlerTest {
	
	// Default messages
	private static final String INTERVAL_RANGE = "The interval (%d) must be in range [%d;%d]";

	private Clock wallClock;
	private RtpClock rtpClock;
	private RtpStatistics statistics;
	private RtcpHandler handler;

	@Before
	public void before() {
		wallClock = new WallTestClock();
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
	
}
