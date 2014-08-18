package org.mobicents.media.server.impl.rtp;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mobicents.media.server.impl.rtp.statistics.RtpStatistics;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class RtpStatisticsTest {

	@Test
	public void testStatisticsCreation() {
//		// given
//		RtpStatistics stats = new RtpStatistics();
//
//		// then
//		assertEquals(0, stats.getRtpPacketsReceived());
//		assertEquals(0, stats.getRtpPacketsSent());
//		assertEquals(0, stats.getSequenceNumber());
//		assertEquals(0, stats.getRtpReceivedOn());
	}

	@Test
	public void testStatisticsIncrement() {
//		// given
//		RtpStatistics stats = new RtpStatistics();
//		Clock clock = new DefaultClock();
//		int cycles = 5;
//
//		// when
//		long clockTime = 0;
//		for (int index = 0; index < cycles; index++) {
//			stats.onRtpReceive();
//			stats.onRtpSent();
//			stats.nextSequenceNumber();
//			clockTime = clock.getTime();
//			stats.setRtpReceivedOn(clockTime);
//		}
//
//		// then
//		assertEquals(cycles, stats.getRtpPacketsReceived());
//		assertEquals(cycles, stats.getRtpPacketsSent());
//		assertEquals(cycles, stats.getSequenceNumber());
//		assertEquals(clockTime, stats.getRtpReceivedOn());
	}

	@Test
	public void testStatisticsReset() {
//		// given
//		RtpStatistics stats = new RtpStatistics();
//		Clock clock = new DefaultClock();
//
//		// when
//		long clockTime = clock.getTime();
//		stats.onRtpReceive();
//		stats.onRtpSent();
//		stats.nextSequenceNumber();
//		stats.setRtpReceivedOn(clockTime);
//		stats.reset();
//
//		// then
//		assertEquals(0, stats.getRtpPacketsReceived());
//		assertEquals(0, stats.getRtpPacketsSent());
//		assertEquals(1, stats.getSequenceNumber());
//		assertEquals(clockTime, stats.getRtpReceivedOn());
	}

}
