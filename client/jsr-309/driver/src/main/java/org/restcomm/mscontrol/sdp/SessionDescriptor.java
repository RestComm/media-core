/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag. 
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.restcomm.mscontrol.sdp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

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
        if (sdp != null) {
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

    public void exclude(MediaType mediaType, Format fmt) {
        for (int i = 0; i < count; i++) {
            if (md[i].getMediaType() == mediaType) {
                md[i].exclude(fmt);
            }
        }
    }

    /**
     * Excludes specified formats from descriptor.
     * 
     * @param formatName the name of the format.
     */
    public void exclude(String formatName) {
        for (int i = 0; i < count; i++) {
            md[i].exclude(formatName);
        }
    }

    /**
     * Checks that specified format is described by this sdp.
     * 
     * @param encoding the encoding name of the format to check
     * @return true if format with specified encoding present in sdp.
     */
    public boolean contains(String encoding) {
        if (encoding.equalsIgnoreCase("sendrecv") || 
                encoding.equalsIgnoreCase("fmtp") ||
                encoding.equalsIgnoreCase("audio") ||
                encoding.equalsIgnoreCase("AS") || 
                encoding.equalsIgnoreCase("IP4")) {
            return true;
        }
        for (int i = 0; i < count; i++) {
            if (md[i].contains(encoding)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean containsMedia(String mediaType) {
        for (int i = 0; i < count; i++) {
            if (md[i].getMediaType().getName().equalsIgnoreCase(mediaType)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        if (origin == null) {
            return "";
        }
        
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
