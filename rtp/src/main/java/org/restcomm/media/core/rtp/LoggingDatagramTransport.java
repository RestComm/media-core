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

package org.restcomm.media.core.rtp;

import java.io.IOException;
import java.io.PrintStream;

import org.bouncycastle.crypto.tls.DatagramTransport;

/**
 * 
 * @author Ivelin Ivanov
 * 
 */
public class LoggingDatagramTransport implements DatagramTransport {

	private static final String HEX_CHARS = "0123456789ABCDEF";

	private final DatagramTransport transport;
	private final PrintStream output;
	private final long launchTimestamp;

	public LoggingDatagramTransport(DatagramTransport transport,
			PrintStream output) {
		this.transport = transport;
		this.output = output;
		this.launchTimestamp = System.currentTimeMillis();
	}

	@Override
	public int getReceiveLimit() throws IOException {
		return transport.getReceiveLimit();
	}

	@Override
	public int getSendLimit() throws IOException {
		return transport.getSendLimit();
	}

	@Override
	public int receive(byte[] buf, int off, int len, int waitMillis)
			throws IOException {
		int length = transport.receive(buf, off, len, waitMillis);
		if (length >= 0) {
			dumpDatagram("Received", buf, off, length);
		}
		return length;
	}

	@Override
	public void send(byte[] buf, int off, int len) throws IOException {
		dumpDatagram("Sending", buf, off, len);
		transport.send(buf, off, len);
	}

	@Override
	public void close() throws IOException {
		// Does nothing
	}

	private void dumpDatagram(String verb, byte[] buf, int off, int len) throws IOException {
		long timestamp = System.currentTimeMillis() - launchTimestamp;
		StringBuffer sb = new StringBuffer("(+").append(timestamp).append("ms ")
				.append(verb).append(" ")
				.append(len).append(" byte datagram:");
		for (int pos = 0; pos < len; ++pos) {
			if (pos % 16 == 0) {
				sb.append(System.getProperty("line.separator"));
				sb.append("    ");
			} else if (pos % 16 == 8) {
				sb.append('-');
			} else {
				sb.append(' ');
			}
			int val = buf[off + pos] & 0xFF;
			sb.append(HEX_CHARS.charAt(val >> 4));
			sb.append(HEX_CHARS.charAt(val & 0xF));
		}
		dump(sb.toString());
	}

	private synchronized void dump(String s) {
		output.println(s);
	}
}
