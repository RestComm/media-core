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

package org.mobicents.media.server.impl.rtcp;

import org.mobicents.media.server.impl.utils.XSRandom;

/**
 * Utility class responsible for calculating intervals for transmission of RTCP
 * reports.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpIntervalCalculator {
	
	/** The minimum interval between transmissions of compound RTCP packets. */
	public static final double MIN_TIME = 5.0;

	/** Initial delay imposed before the first compound RTCP packet is sent. */
	public static final double INITIAL_MIN_TIME = MIN_TIME / 2.0;
	
	/** "timer reconsideration": converges to a value below the intended average */
	private static final double COMPENSATION = Math.E - (3.0 / 2.0);
	
	private static final XSRandom RANDOM = new XSRandom();

	/**
	 * <b>6.3.1 - Computing the RTCP Transmission Interval</b>
	 * 
	 * <p>
	 * To maintain scalability, the average interval between packets from a
	 * session participant should scale with the group size. This interval is
	 * called the calculated interval. It is obtained by combining a number of
	 * the pieces of state described above. The calculated interval T is then
	 * determined as follows:
	 * </p>
	 * <p>
	 * 1. If the number of senders is less than or equal to 25% of the
	 * membership (members), the interval depends on whether the participant is
	 * a sender or not (based on the value of we_sent). If the participant is a
	 * sender (we_sent true), the constant C is set to the average RTCP packet
	 * size (avg_rtcp_size) divided by 25% of the RTCP bandwidth (rtcp_bw), and
	 * the constant n is set to the number of senders. If we_sent is not true,
	 * the constant C is set to the average RTCP packet size divided by 75% of
	 * the RTCP bandwidth. The constant n is set to the number of receivers
	 * (members - senders). If the number of senders is greater than 25%,
	 * senders and receivers are treated together. The constant C is set to the
	 * average RTCP packet size divided by the total RTCP bandwidth and n is set
	 * to the total number of members. As stated in Section 6.2, an RTP profile
	 * MAY specify that the RTCP bandwidth may be explicitly defined by two
	 * separate parameters (call them S and R) for those participants which are
	 * senders and those which are not. In that case, the 25% fraction becomes
	 * S/(S+R) and the 75% fraction becomes R/(S+R). Note that if R is zero, the
	 * percentage of senders is never greater than S/(S+R), and the
	 * implementation must avoid division by zero.
	 * </p>
	 * <p>
	 * 2. If the participant has not yet sent an RTCP packet (the variable
	 * initial is true), the constant Tmin is set to 2.5 seconds, else it is set
	 * to 5 seconds.
	 * </p>
	 * <p>
	 * 3. The deterministic calculated interval Td is set to max(Tmin, n*C).
	 * </p>
	 * <p>
	 * 4. The calculated interval T is set to a number uniformly distributed
	 * between 0.5 and 1.5 times the deterministic calculated interval.
	 * </p>
	 * <p>
	 * 5. The resulting value of T is divided by e-3/2=1.21828 to compensate for
	 * the fact that the timer reconsideration algorithm converges to a value of
	 * the RTCP bandwidth below the intended average.
	 * </p>
	 * <p>
	 * This procedure results in an interval which is random, but which, on
	 * average, gives at least 25% of the RTCP bandwidth to senders and the rest
	 * to receivers. If the senders constitute more than one quarter of the
	 * membership, this procedure splits the bandwidth equally among all
	 * participants, on average.
	 * </p>
	 * 
	 * @param initial
	 * @param weSent
	 * @param senders
	 * @param members
	 * @param rtcpAvgSize
	 * @param rtcpBw
	 * @param rtcpBwFraction
	 * 
	 * @return the new transmission interval, in milliseconds
	 */
	public static long calculateInterval(boolean initial, boolean weSent,
			int senders, int members, double rtcpAvgSize, double rtcpBw,
			double rtcpBwFraction, double senderBwFraction,
			double receiverBwFraction) {
		// 1 - calculate n and c
		double c;
		int n;
		
		if (senders <= (members * senderBwFraction)) {
			if (weSent) {
				c = rtcpAvgSize / (senderBwFraction * rtcpBw);
				n = senders;
			} else {
				c = rtcpAvgSize / (receiverBwFraction * rtcpBw);
				n = members - senders;
			}
		} else {
			c = rtcpAvgSize / rtcpBw;
			n = members;
		}

		// 2 - calculate Tmin
		double tMin = initial ? INITIAL_MIN_TIME : MIN_TIME;

		// 3 - calculate Td
		double td = Math.max(tMin, n * c);

		// 4 - calculate interval T
		double min = td * 0.5;
		double max = td * 1.5;
		double t = min + (max - min) * RANDOM.nextDouble();

		// 5 - divide T by e-3/2 and convert to milliseconds
		return (long) ((t / COMPENSATION) * 1000);
	}

}
