package org.mobicents.media.server.impl.rtp.statistics;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.apache.commons.net.ntp.TimeStamp;
import org.junit.Test;
import org.mobicents.media.server.impl.rtcp.RtcpSenderReport;
import org.mobicents.media.server.impl.rtcp.ntp.NtpUtils;
import org.mobicents.media.server.impl.rtp.RtpClock;
import org.mobicents.media.server.impl.rtp.RtpPacket;
import org.mobicents.media.server.impl.rtp.WallTestClock;

/**
 * Unit tests for {@link RtpMember}
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
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
			sum += packet.getPayloadLength();
		}
		return sum;
	}
	
	private void receiveRtpPackets(RtpMember member, RtpPacket ...packets) {
		for (RtpPacket packet : packets) {
			member.onReceiveRtp(packet);
			wallClock.tick(20000);
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
		// packet is still in probation so all values are zero
		assertEquals(p1.getSyncSource(), member.getSsrc());
		assertEquals(0, member.getOctetsReceived());
		assertEquals(0, member.getPacketsReceived());
		assertEquals(0, member.getReceivedSinceSR());
		assertEquals(0, member.getPacketsLost());
		assertEquals(0, member.getExtHighSequence());
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
		RtpPacket p4 = new RtpPacket(172, false);
		p1.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 2, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 3, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 3, 160 * 4, 123, new byte[160], 0, 160);

		// when
		receiveRtpPackets(member, p1, p2, p3, p4);

		// then
		assertEquals(p1.getSyncSource(), member.getSsrc());
		assertEquals(sumOctets(p3, p4), member.getOctetsReceived());
		assertEquals(2, member.getPacketsReceived());
		assertEquals(2, member.getReceivedSinceSR());
		assertEquals(-1, member.getPacketsLost()); // expected 1 packet but received 2 (with same seqnum)
		assertEquals(p4.getSeqNumber(), member.getExtHighSequence());
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
		p2.wrap(false, 8, 101, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 102, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 103, 160 * 4, 123, new byte[160], 0, 160);
		// The 100+ sequence number gap will cause the sequence cycle to increment
		p5.wrap(false, 8, 50, 160 * 5, 123, new byte[160], 0, 160);
		
		// when
		receiveRtpPackets(member, p1, p2, p3, p4, p5);
		
		// then
		int expectedSeqCycle = 1;
		assertEquals(expectedSeqCycle, member.getSequenceCycle());

		int expectedHighSequence = (expectedSeqCycle * RtpMember.RTP_SEQ_MOD) + p5.getSeqNumber();
		assertEquals(expectedHighSequence, member.getExtHighSequence());
		int expected = expectedHighSequence - p1.getSeqNumber() - 1; // discard last packet hence the -1
		long expectedLostPackets = expected - member.getPacketsReceived();
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
		RtpPacket p6 = new RtpPacket(172, false);
		RtpPacket p7 = new RtpPacket(172, false);

		// packets in probation
		p6.wrap(false, 8, 1, 160 * 1, 123, new byte[160], 0, 160);
		p7.wrap(false, 8, 3, 160 * 1, 123, new byte[160], 0, 160);
		
		// valid packets
		p1.wrap(false, 8, 3, 160 * 1, 123, new byte[160], 0, 160);
		p2.wrap(false, 8, 4, 160 * 2, 123, new byte[160], 0, 160);
		p3.wrap(false, 8, 5, 160 * 3, 123, new byte[160], 0, 160);
		p4.wrap(false, 8, 6, 160 * 4, 123, new byte[160], 0, 160);
		p5.wrap(false, 8, 7, 160 * 5, 123, new byte[160], 0, 160);

		// 1 sampling units delta for timing and rounding errors , i.e. 1/8ms
		long jitterDeltaLimit = 1;

		/*
		 * When/Then
		 */
		// send two dummy ordered packets to pass probation period
		member.onReceiveRtp(p6);
		member.onReceiveRtp(p7);
		
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
		long expectedSrTimestamp = NtpUtils.calculateLastSrTimestamp(ntp.getSeconds(), ntp.getFraction());
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
		long expectedHighSeq = (65536 * 0 + p5.getSeqNumber());
		assertEquals(expectedHighSeq, member.getExtHighSequence());
		assertEquals(3, member.getPacketsReceived());
		assertEquals(sumOctets(p3, p4, p5), member.getOctetsReceived());
		assertEquals(1, member.getReceivedSinceSR());
		assertEquals(0, member.getPacketsLost());
		assertEquals(0, member.getFractionLost()); // expected packets = received packets = no loss
		long expectedDelay = (long) ((wallClock.getCurrentTime() - receivedSrOn) * 65.536);
		assertEquals(expectedDelay, member.getLastSRdelay());
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
		int packetsReceived = 3; // 2 first packets were in probation period
		int expected = p5.getSeqNumber() - p3.getSeqNumber() + 1; // only considers valid sequence after 2 consecutive packets
		long expectedLostPackets = expected - packetsReceived;
		long lostInterval = expected - packetsReceived;
		long fractionLost = (lostInterval * 256) / expected;
		
		assertEquals(expectedLostPackets, member.getPacketsLost());
		assertEquals(fractionLost, member.getFractionLost());
	}

}
