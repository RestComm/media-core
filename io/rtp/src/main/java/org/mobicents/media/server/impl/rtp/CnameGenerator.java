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

package org.mobicents.media.server.impl.rtp;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.bouncycastle.util.encoders.Base64;

/**
 * Generates unique CNAME between different RTP sessions.
 * <p>
 * The mechanism used to generate CNAME is described in <a
 * href="http://tools.ietf.org/html/rfc7022">RFC 7022</a>, instead of the
 * classic approach suggested by <a
 * href="http://tools.ietf.org/html/rfc3550">RFC 3550</a>.
 * </p>
 * <p>
 * The reason behind this choice is that it is difficult, and in some cases
 * impossible, for a host to determine if there is a NAT between itself and its
 * RTP peer. Furthermore, even some public IPv4 addresses can be shared by
 * multiple hosts in the Internet. Using the numeric representation of the IPv4
 * address as the "host" part of the RTCP CNAME is NOT RECOMMENDED!
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class CnameGenerator {

	/**
	 * For every new RTP session, a new RTCP CNAME is created by generating a
	 * cryptographically pseudorandom value as described in [RFC4086]. This
	 * value MUST be at least 96 bits.
	 * <p>
	 * After performing that procedure, minimally the least significant 96 bits
	 * SHOULD be converted to ASCII using Base64 encoding [RFC4648]. The RTCP
	 * CNAME cannot change over the life of an RTP session [RFC3550]. The
	 * "user@" part of the RTCP CNAME is omitted when generating per-session
	 * RTCP CNAMEs.
	 * </p>
	 * 
	 * @return
	 */
	public static String generateCname() {
		// generate unique identifier
		UUID uuid = UUID.randomUUID();
		// get the 64 least significant bits 
		long leastSignificantBits = uuid.getLeastSignificantBits();
		// get the 64 most significant bits 
		long mostSignificantBits = uuid.getMostSignificantBits();
		
		// convert the 128 bits to byte array
		// note: 128 / 8 = 16 bytes
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.putLong(leastSignificantBits).putLong(mostSignificantBits);
		buffer.flip();

		// get the 96 least significant bits
		// note: 96 / 8 = 12
		byte[] data = new byte[12];
		buffer.get(data, 0, 12);
		
		// convert the least 96 bits to ASCII Base64
		return Base64.toBase64String(data);
	}

}