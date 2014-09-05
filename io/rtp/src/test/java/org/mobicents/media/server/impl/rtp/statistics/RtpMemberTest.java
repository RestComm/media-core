package org.mobicents.media.server.impl.rtp.statistics;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.apache.commons.net.ntp.TimeStamp;
import org.junit.Test;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.WallTestClock;

public class RtpMemberTest {
	
	private final WallTestClock wallClock;
	private final RtpClock rtpClock;

	public RtpMemberTest() {
		this.wallClock = new WallTestClock();
		this.rtpClock = new RtpClock(this.wallClock);
		this.rtpClock.setClockRate(8000);
	}

	private int sumOctets(RtpPacket ...packets) {
		int sum = 0;
		for (RtpPacket packet : packets) {
			sum += packet.getLength();
		}
		return sum;
	}
	
	private void receiveRtpPackets(RtpMember member, RtpPacket ...packets) {
		for (RtpPacket packet : packets) {
			member.onReceiveRtp(packet);
		}
	}

	@Test
	public void testOnReceiveRtpPacket() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		RtpPacket p1 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);

		// when
		member.onReceiveRtp(p1);

		// then
		assertEquals(p1.getSyncSource(), member.getSsrc());
		assertEquals(p1.getLength(), member.getOctetsReceived());
		assertEquals(1, member.getPacketsReceived());
		assertEquals(1, member.getReceivedSinceSR());
		assertEquals(0, member.getPacketsLost());
		assertEquals(p1.getSeqNumber(), member.getSequenceNumber());
		assertEquals(p1.getSeqNumber(), member.getExtHighSequence());
		assertEquals(0, member.getSequenceCycle());
		assertEquals(0, member.getLastSR());
		assertEquals(0, member.getLastSRdelay());
	}

	@Test
	public void testOnReceiveRtpPackets() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		RtpPacket p3 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 2, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 2, 160 * 3, 123, new byte[160], 0, 160);
		

		// when
		receiveRtpPackets(member, p1, p2, p3);

		// then
		assertEquals(p1.getSyncSource(), member.getSsrc());
		assertEquals(sumOctets(p1, p2, p3), member.getOctetsReceived());
		assertEquals(3, member.getPacketsReceived());
		assertEquals(3, member.getReceivedSinceSR());
		assertEquals(0, member.getPacketsLost());
		assertEquals(p3.getSeqNumber(), member.getSequenceNumber());
		assertEquals(p3.getSeqNumber(), member.getExtHighSequence());
		assertEquals(0, member.getSequenceCycle());
		assertEquals(0, member.getLastSR());
		assertEquals(0, member.getLastSRdelay());
	}

	@Test
	public void testSequenceCycleIncrement() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		RtpPacket p3 = new RtpPacket(172, false);
		RtpPacket p4 = new RtpPacket(172, false);
		RtpPacket p5 = new RtpPacket(172, false);
		
		p1.wrap(false, 8, 100, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 120, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 150, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 155, 160 * 4, 123, new byte[160], 0, 160);
		// The 100+ sequence number gap will cause the sequence cycle to increment
		p5.wrap(false, 8, 54, 160 * 5, 123, new byte[160], 0, 160);
		
		// when
		receiveRtpPackets(member, p1, p2, p3, p4, p5);
		
		// then
		int expectedSeqCycle = 1;
		assertEquals(expectedSeqCycle, member.getSequenceCycle());
		assertEquals(p5.getSeqNumber(), member.getSequenceNumber());

		long expectedHighSequence = (65536 * expectedSeqCycle) + p5.getSeqNumber();
		assertEquals(expectedHighSequence, member.getExtHighSequence());
		
		long expectedLostPackets = expectedHighSequence - p1.getSeqNumber() - member.getPacketsReceived();
		if(expectedLostPackets < 0) {
			expectedLostPackets = 0;
		}
		assertEquals(expectedLostPackets, member.getPacketsLost());
	}
	
	@Test
	public void testJitter() {
		/*
		 * Given
		 */
		RtpMember member = new RtpMember(rtpClock, 123);
		
		// the timestamp for each packet increases by 10ms=160 timestamp units
		// for sampling rate 8KHz
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		RtpPacket p3 = new RtpPacket(172, false);
		RtpPacket p4 = new RtpPacket(172, false);
		RtpPacket p5 = new RtpPacket(172, false);

		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 2, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 2, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 3, 160 * 4, 123, new byte[160], 0, 160);
		p5.wrap(false, 8, 3, 160 * 5, 123, new byte[160], 0, 160);

		// 1 sampling units delta for timing and rounding errors , i.e. 1/8ms
		long jitterDeltaLimit = 1;

		/*
		 * When/Then
		 */
		// write first packet, expected jitter = 0
		member.onReceiveRtp(p1);
		assertEquals(0, member.getJitter(), jitterDeltaLimit);

		// move time forward by 20ms and write the second packet
		// the transit time should remain approximately the same - near 0ms.
		// expected jitter = 0;
		wallClock.tick(20000000L);
		member.onReceiveRtp(p2);
		assertEquals(0, member.getJitter(), jitterDeltaLimit);

		// move time forward by 30ms and write the next packet
		// the transit time should increase by 10ms,
		// as suggested by the difference in the third packet timestamp (160*3)
		// and the 20ms delay for the server to receive the second packet
		// expected jitter should be close to the 10ms delay in timestamp
		// units/16, i.e. 80/16.
		wallClock.tick(30000000L);
		member.onReceiveRtp(p3);
		assertEquals(5, member.getJitter(), jitterDeltaLimit);

		// move time forward by 20ms and write the next packet
		// the transit time does not change from the previous packet.
		// The jitter should stay approximately the same.
		wallClock.tick(20000000L);
		member.onReceiveRtp(p4);
		assertEquals(4, member.getJitter(), jitterDeltaLimit);

		// move time forward by 30ms and write the next packet
		// packet was delayed 10ms again.
		// The estimated jitter should increase significantly, by nearly 5ms
		// (80/16)
		wallClock.tick(30000000L);
		member.onReceiveRtp(p5);
		assertEquals(9, member.getJitter(), jitterDeltaLimit);
	}
	
	@Test
	public void testOnReceiveSR() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		
		TimeStamp ntp = new TimeStamp(new Date());
		RtcpSenderReport sendReport = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 2, 100, 100 * 130);
		
		long receiveTime = this.wallClock.getCurrentTime();
		
		// when
		member.onReceiveSR(sendReport);
		this.wallClock.tick(20000000L);
		
		// then
		long expectedSrTimestamp = RtpMember.calculateLastSrTimestamp(ntp.getSeconds());
		assertEquals(expectedSrTimestamp, member.getLastSR());
		assertEquals(0, member.getReceivedSinceSR());
		
		long expectedDelay = (long) ((this.wallClock.getCurrentTime() - receiveTime) * 65.536);
		assertEquals(expectedDelay, member.getLastSRdelay());
	}
	
	@Test
	public void testOnReceiveRtpAndSR() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		RtpPacket p3 = new RtpPacket(172, false);
		RtpPacket p4 = new RtpPacket(172, false);
		RtpPacket p5 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 2, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 3, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 4, 160 * 4, 123, new byte[160], 0, 160);
		p5.wrap(false, 8, 5, 160 * 5, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(new Date());
		RtcpSenderReport sendReport = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 2, 100, 100 * 130);
		
		// when
		member.onReceiveRtp(p1);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p2);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p3);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p4);
		wallClock.tick(20000000L);
		long receivedSrOn = this.wallClock.getCurrentTime();
		member.onReceiveSR(sendReport);
		member.onReceiveRtp(p5);
		wallClock.tick(20000000L);
		
		// then
		assertEquals(0, member.getSequenceCycle());
		assertEquals(p5.getSeqNumber(), member.getSequenceNumber());
		long expectedHighSeq = (65536 * 0 + p5.getSeqNumber());
		assertEquals(expectedHighSeq, member.getExtHighSequence());
		assertEquals(5, member.getPacketsReceived());
		assertEquals(sumOctets(p1, p2, p3, p4, p5), member.getOctetsReceived());
		assertEquals(1, member.getReceivedSinceSR());
		long expectedPackets = p5.getSeqNumber() - p4.getSeqNumber();
		long expectedFraction = (256 * (expectedPackets - 1)) / expectedPackets;
		assertEquals(expectedFraction, member.getFractionLost());
		long expectedDelay = (long) ((wallClock.getCurrentTime() - receivedSrOn) * 65.536);
		assertEquals(expectedDelay, member.getLastSRdelay());
		long expectedLost = expectedHighSeq - p1.getSeqNumber() - member.getPacketsReceived();
		assertEquals(expectedLost < 0 ? 0 : expectedLost, member.getPacketsLost());
	}
	
	@Test
	public void testLostPackets() {
		// given
		RtpMember member = new RtpMember(rtpClock, 123);
		
		RtpPacket p1 = new RtpPacket(172, false);
		RtpPacket p2 = new RtpPacket(172, false);
		RtpPacket p3 = new RtpPacket(172, false);
		RtpPacket p4 = new RtpPacket(172, false);
		RtpPacket p5 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 2, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 3, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 4, 160 * 4, 123, new byte[160], 0, 160);
		p5.wrap(false, 8, 24, 160 * 5, 123, new byte[160], 0, 160);
		
		TimeStamp ntp = new TimeStamp(new Date());
		RtcpSenderReport sendReport = new RtcpSenderReport(false, 123, ntp.getSeconds(), ntp.getFraction(), 160 * 2, 100, 100 * 130);
		
		// when
		member.onReceiveRtp(p1);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p2);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p3);
		wallClock.tick(20000000L);
		member.onReceiveRtp(p4);
		wallClock.tick(20000000L);
		member.onReceiveSR(sendReport);
		member.onReceiveRtp(p5);
		wallClock.tick(20000000L);
		
		// then
		long expectedPackets = p5.getSeqNumber() - p4.getSeqNumber();
		int receivedSinceSR = 1; // p5
		long expectedFraction = (256 * (expectedPackets - receivedSinceSR)) / expectedPackets;
		assertEquals(expectedFraction, member.getFractionLost());
	}
}
