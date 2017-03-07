/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.resource.dtmf.util;

/**
 * 
 * @author amit bhayani
 *
 */
public class NtpTimeStampUtil {

	/**
	 * baseline NTP time if bit-0=0 -> 7-Feb-2036 @ 06:28:16 UTC
	 */
	private static final long msb0baseTime = 2085978496000L;

	/**
	 * baseline NTP time if bit-0=1 -> 1-Jan-1900 @ 01:00:00 UTC
	 */
	private static final long msb1baseTime = -2208988800000L;

	private static long[] time = new long[2];

	public static long getTime(long seconds, long fraction) {

		// Use round-off on fractional part to preserve going to lower precision
		fraction = Math.round(1000D * fraction / 0x100000000L);

		long msb = seconds & 0x80000000L;

		if (msb == 0) {
			// use base: 7-Feb-2036 @ 06:28:16 UTC
			return msb0baseTime + (seconds * 1000) + fraction;
		} else {
			// use base: 1-Jan-1900 @ 01:00:00 UTC
			return msb1baseTime + (seconds * 1000) + fraction;
		}
	}

	public static long[] toNtpTime(long t) {
		boolean useBase1 = t < msb0baseTime; // time < Feb-2036
		long baseTime;
		if (useBase1) {
			baseTime = t - msb1baseTime; // dates <= Feb-2036
		} else {
			// if base0 needed for dates >= Feb-2036
			baseTime = t - msb0baseTime;
		}

		long seconds = baseTime / 1000;
		long fraction = ((baseTime % 1000) * 0x100000000L) / 1000;

		if (useBase1) {
			seconds |= 0x80000000L; // set high-order bit if msb1baseTime 1900 used
		}

		time[0] = seconds;
		time[1] = fraction;

		return time;
	}
}
