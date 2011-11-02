/*
 * JBoss, Home of Professional Open Source
 * Copyright XXXX, Red Hat Middleware LLC, and individual contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a full listing
 * of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU General Public License, v. 2.0.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License,
 * v. 2.0 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package org.mobicents.media.server.impl.rtp.sdp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.mobicents.media.server.spi.MediaType;

/**
 * 
 * @author kulikov
 * @author amit bhayani
 */
public class SessionDescriptor {

	private String version;
	private Origin origin;
	private String session;
	private Connection connection;

	private MediaDescriptor md[] = new MediaDescriptor[15];
	private int count;

	public SessionDescriptor(String sdp) {
		this(sdp, true);
	}

	public SessionDescriptor(String sdp, boolean processMandatoryFields) {

		BufferedReader reader = init(sdp);
		try {
			String line = reader.readLine();
			if (processMandatoryFields) {

				int pos = line.indexOf('=') + 1;
				version = line.substring(pos, line.length());

				// parse origin
				line = reader.readLine();
				origin = new Origin(line);

				// parse session
				line = reader.readLine();
				pos = line.indexOf('=') + 1;
				session = line.substring(pos, line.length());

				// From here on rest of the lines are optional but time and
				// media
				// description
				line = next(reader);
			}

			char c;
			while (line != null) {
				c = line.charAt(0);
				switch (c) {
				// c
				case 99:
					// parse connection
					connection = new Connection(line);
					break;
				// t
				case 116:
					break;
				// m
				case 109:
					md[count++] = new MediaDescriptor(line);
					line = parseAttributes(reader);
					continue;
				}

				line = next(reader);
			}

		} catch (IOException e) {
		}

	}

	private String parseAttributes(BufferedReader reader) throws IOException {
		String line = next(reader);
		while (line != null && !line.startsWith("m=")) {
			md[count - 1].parseAtribute(line);
			line = next(reader);
		}
		return line;
	}

	private String next(BufferedReader reader) throws IOException {
		return reader.readLine();
	}

	private BufferedReader init(String s) {
		return new BufferedReader(new StringReader(s));
	}

	public SessionDescriptor() {
		this.version = "0";
	}

	public String getVersion() {
		return version;
	}

	public Origin getOrigin() {
		return origin;
	}

	public String getSession() {
		return session;
	}

	public void setSession(String session) {
		this.session = session;
	}

	public Connection getConnection() {
		return connection;
	}

	public int getMediaTypeCount() {
		return count;
	}

	public MediaDescriptor getMediaDescriptor(int i) {
		return md[i];
	}

	public MediaDescriptor getMediaDescriptor(MediaType mediaType) {
		for (int i = 0; i < count; i++) {
			if (md[i].mediaType == mediaType) {
				return md[i];
			}
		}
		return null;
	}

	public void createOrigin(String name, String sessionID,
			String sessionVersion, String networkType, String addressType,
			String address) {
		origin = new Origin(name, sessionID, sessionVersion, networkType,
				addressType, address);
	}

	public void createConnection(String networkType, String addressType,
			String address) {
		connection = new Connection(networkType, addressType, address);
	}

	public MediaDescriptor addMedia(MediaType mediaType, int port) {
		md[count++] = new MediaDescriptor(mediaType, port);
		return md[count - 1];
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("v=" + version + "\n");
		buffer.append(origin.toString() + "\n");
		buffer.append("s=" + session + "\n");
		buffer.append(connection.toString() + "\n");
		buffer.append("t=0 0\n");

		for (int i = 0; i < count; i++) {
			md[i].write(buffer);
		}
		return buffer.toString();
	}
}
