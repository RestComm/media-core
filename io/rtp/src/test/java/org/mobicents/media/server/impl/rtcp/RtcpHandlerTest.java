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
	public void testJoinAndLeaveRtpSession() {
		// given
		// we_sent == false, then C = avg_rtcp_size / (rtcp_bw * 0.75)
		double c = statistics.getRtcpAvgSize() / (RtpStatistics.RTCP_DEFAULT_BW * 0.75);
		// n = num_receivers = members - senders
		int n = statistics.getMembers() - statistics.getSenders();
		// initial == true, then Tmin = 2.5 seconds
		double tMin = RtpStatistics.INITIAL_RTCP_MIN_TIME;
		double tD = Math.max(tMin, n * c);
		// T = [Td * 0.5; Td * 1.5]
		// t = T / (e - 3/2)
		double compensation = Math.E - (3.0 / 2.0);
		double lowT = (tD * 0.5) / compensation;
		double highT = (tD * 1.5) / compensation;

		// when
		handler.joinRtpSession();
		long nextReport = handler.getNextScheduledReport();

		// then
		Assert.assertTrue(handler.isJoined());
		Assert.assertTrue(nextReport >= (long) (lowT * 1000));
		Assert.assertTrue(nextReport <= (long) (highT * 1000));
	}
	
}
