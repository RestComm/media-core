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

package org.restcomm.media.core.rtcp;

import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.core.rtcp.RtcpIntervalCalculator;

/**
 * Tests the algorithm to calculate sending intervals for RTCP reports.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpIntervalCalculatorTest {
	
	// Default values
	public static final int RTP_BW = 8000;
	public static final double RTCP_BW_FRACTION = 0.05;
	public static final double RTCP_BW = RTP_BW * RTCP_BW_FRACTION;
	public static final double RTCP_SENDER_BW_FRACTION = 0.25;
	public static final double RTCP_RECEIVER_BW_FRACTION = 1.0 - RTCP_SENDER_BW_FRACTION;
	public static final double RTCP_AVG_SIZE = 200.0;
	
	// Default messages
	private static final String INTERVAL_RANGE = "The interval (%d) must be in range [%d;%d]";

	@Test
	public void testInitialReceiverInterval() {
		// GIVEN
		boolean initial = true;
		// simulate two participants: remote is sender, local is receiver
		boolean weSent = false;
		int members = 2;
		int senders = 1;

		for(int i = 0; i < 1000; i++) {
			// WHEN
			long interval = RtcpIntervalCalculator.calculateInterval(initial,
					weSent, senders, members, RTCP_AVG_SIZE, RTCP_BW,
					RTCP_BW_FRACTION, RTCP_SENDER_BW_FRACTION,
					RTCP_RECEIVER_BW_FRACTION);
			
			// THEN
			// senders > members * 0.25, then does not impact C nor N
			double c = RTCP_AVG_SIZE / RTCP_BW;
			int n = members;
			// initial == true, then Tmin = 2.5 seconds
			double tMin = RtcpIntervalCalculator.INITIAL_MIN_TIME;
			double tD = Math.max(tMin, n * c);
			// T = [Td * 0.5; Td * 1.5]
			// t = T / (e - 3/2)
			// Values will be converted to milliseconds (* 1000)
			double compensation = Math.E - (3.0 / 2.0);
			long lowT = (long) (((tD * 0.5) / compensation) * 1000);
			long highT = (long) (((tD * 1.5) / compensation) * 1000);
			
			String msg = String.format(INTERVAL_RANGE, interval, lowT, highT);
			Assert.assertTrue(msg, interval >= lowT);
			Assert.assertTrue(msg, interval <= highT);
		}
	}

	@Test
	public void testReceiverIntervalWithSenderMinority() {
		// GIVEN
		// no rtcp packet has been sent yet
		boolean initial = false;
		// simulate 4 participants: only one is sender (which affects variables), local is receiver
		boolean weSent = false;
		int members = 4;
		int senders = 1;
		
		for(int i = 0; i < 1000; i++) {
			// WHEN
			long interval = RtcpIntervalCalculator.calculateInterval(initial,
					weSent, senders, members, RTCP_AVG_SIZE, RTCP_BW,
					RTCP_BW_FRACTION, RTCP_SENDER_BW_FRACTION,
					RTCP_RECEIVER_BW_FRACTION);
			
			// THEN
			// senders <= members * 0.25, then DOES impact C nor N
			// we_sent == false, then C = avg_rtcp_size / (rtcp_bw * 0.75)
			double c = RTCP_AVG_SIZE / (RTCP_BW * RTCP_RECEIVER_BW_FRACTION);
			// we_sent == false, then n = num_receivers = members - senders
			int n = members - senders;
			// initial == false, then Tmin = 5 seconds
			double tMin = RtcpIntervalCalculator.MIN_TIME;
			double tD = Math.max(tMin, n * c);
			// T = [Td * 0.5; Td * 1.5]
			// t = T / (e - 3/2)
			// Values will be converted to milliseconds (* 1000)
			double compensation = Math.E - (3.0 / 2.0);
			long lowT = (long) (((tD * 0.5) / compensation) * 1000);
			long highT = (long) (((tD * 1.5) / compensation) * 1000);
			
			String msg = String.format(INTERVAL_RANGE, interval, lowT, highT);
			Assert.assertTrue(msg, interval >= lowT);
			Assert.assertTrue(msg, interval <= highT);
		}
	}
	
	@Test
	public void testInitialSenderIntervalWithSenderMinority() {
		// GIVEN
		// no rtcp packet has been sent yet
		boolean initial = true;
		// simulate 8 participants: only three are senders (which affects variables), local is sender
		boolean weSent = true;
		int members = 12;
		int senders = 3;
		
		for(int i = 0; i < 1000; i++) {
			// WHEN
			long interval = RtcpIntervalCalculator.calculateInterval(initial,
					weSent, senders, members, RTCP_AVG_SIZE, RTCP_BW,
					RTCP_BW_FRACTION, RTCP_SENDER_BW_FRACTION,
					RTCP_RECEIVER_BW_FRACTION);
			
			// THEN
			// senders <= members * 0.25, then DOES impact C nor N
			// we_sent == false, then C = avg_rtcp_size / (rtcp_bw * 0.25)
			double c = RTCP_AVG_SIZE / (RTCP_BW * RTCP_SENDER_BW_FRACTION);
			// we_sent == true, then n = senders
			int n = senders;
			// initial == true, then Tmin = 2.5 seconds
			double tMin = RtcpIntervalCalculator.INITIAL_MIN_TIME;
			double tD = Math.max(tMin, n * c);
			// T = [Td * 0.5; Td * 1.5]
			// t = T / (e - 3/2)
			// Values will be converted to milliseconds (* 1000)
			double compensation = Math.E - (3.0 / 2.0);
			long lowT = (long) (((tD * 0.5) / compensation) * 1000);
			long highT = (long) (((tD * 1.5) / compensation) * 1000);
			
			String msg = String.format(INTERVAL_RANGE, interval, lowT, highT);
			Assert.assertTrue(msg, interval >= lowT);
			Assert.assertTrue(msg, interval <= highT);
		}
	}

	@Test
	public void testSenderIntervalWithSenderMinority() {
		// GIVEN
		// no rtcp packet has been sent yet
		boolean initial = false;
		// simulate 8 participants: only three are senders (which affects variables), local is sender
		boolean weSent = true;
		int members = 8;
		int senders = 2;
		
		for(int i = 0; i < 1000; i++) {
			// WHEN
			long interval = RtcpIntervalCalculator.calculateInterval(initial,
					weSent, senders, members, RTCP_AVG_SIZE, RTCP_BW,
					RTCP_BW_FRACTION, RTCP_SENDER_BW_FRACTION,
					RTCP_RECEIVER_BW_FRACTION);
			
			// THEN
			// senders <= members * 0.25, then DOES impact C nor N
			// we_sent == false, then C = avg_rtcp_size / (rtcp_bw * 0.25)
			double c = RTCP_AVG_SIZE / (RTCP_BW * RTCP_SENDER_BW_FRACTION);
			// we_sent == true, then n = senders
			int n = senders;
			// initial == false, then Tmin = 5 seconds
			double tMin = RtcpIntervalCalculator.MIN_TIME;
			double tD = Math.max(tMin, n * c);
			// T = [Td * 0.5; Td * 1.5]
			// t = T / (e - 3/2)
			// Values will be converted to milliseconds (* 1000)
			double compensation = Math.E - (3.0 / 2.0);
			long lowT = (long) (((tD * 0.5) / compensation) * 1000);
			long highT = (long) (((tD * 1.5) / compensation) * 1000);
			
			String msg = String.format(INTERVAL_RANGE, interval, lowT, highT);
			Assert.assertTrue(msg, interval >= lowT);
			Assert.assertTrue(msg, interval <= highT);
		}
	}

}
