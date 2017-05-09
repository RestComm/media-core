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
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.pcap;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import net.ripe.hadoop.pcap.PcapReader;
import net.ripe.hadoop.pcap.packet.Packet;

/**
 * Loads an existing PCAP file from local filesystem or network so the user can perform read operations.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PcapFile implements Closeable {

    private static final String PCAP_REGEX = ".+\\.p?cap(.gz(ip)?)?";
    private static final String GZIP_REGEX = ".+\\.gz(ip)?";

    private final URL path;
    private final boolean compressed;

    private InputStream inputStream;
    private PcapReader reader;

    public PcapFile(URL path) {
        if (!path.toString().matches(PCAP_REGEX)) {
            throw new IllegalArgumentException("Unsupported extension: " + path.toString());
        }
        this.path = path;
        this.compressed = path.toString().matches(GZIP_REGEX);
    }
    
    public URL getPath() {
        return path;
    }

    public void open() throws IOException {
        this.inputStream = this.path.openStream();
        if (this.compressed) {
            this.inputStream = new GZIPInputStream(inputStream);
        }
        this.reader = new GenericPcapReader(new DataInputStream(inputStream));
    }

    public Packet read() {
        if (this.reader != null) {
            Iterator<Packet> iterator = this.reader.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return null;
    }
    
    public boolean isComplete() {
        return !this.reader.iterator().hasNext();
    }

    @Override
    public void close() throws IOException {
        if (this.reader != null) {
            this.reader = null;
        }
        if (this.inputStream != null) {
            this.inputStream.close();
        }
    }

}
