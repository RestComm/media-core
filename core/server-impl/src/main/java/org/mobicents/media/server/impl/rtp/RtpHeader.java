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
package org.mobicents.media.server.impl.rtp;

import java.io.Serializable;

/**
 * 
 * @author Oleg Kulikov
 */
public class RtpHeader implements Serializable {

    private int version = 2;
    private boolean padding = false;
    private boolean extensions = false;
    private int cc = 0;
    private boolean marker = false;
    private int payloadType;
    private int seqNumber;
    private long timestamp;
    private long ssrc;
    private byte[] bin = new byte[12];
    private int pos = 0;
    private boolean filled = false;

    public void RtpHeader() {
    }

    public boolean isFilled() {
        return filled;
    }

    public void init(byte[] bin) {
        this.bin = bin;
        int b = bin[0] & 0xff;
        
        version = (b & 0x0C) >> 6;
        padding = (b & 0x20) == 0x020;
        extensions = (b & 0x10) == 0x10;
        cc = b & 0x0F;

        b = bin[1] & 0xff;

        marker = (b & 0x80) == 0x80;
        payloadType = b & 0x7F;

        seqNumber = (bin[2] & 0xff) << 8;
        seqNumber = seqNumber | (bin[3] & 0xff);

        timestamp = (bin[4] & 0xff);
        timestamp = (timestamp << 8) | (bin[5] & 0xff);
        timestamp = (timestamp << 8) | (bin[6] & 0xff);
        timestamp = (timestamp << 8) | (bin[7] & 0xff);
        
        ssrc = (bin[8] & 0xff);
        ssrc = (ssrc << 8) | (bin[9] & 0xff);
        ssrc = (ssrc << 8) | (bin[10] & 0xff);
        ssrc = (ssrc << 8) | (bin[11] & 0xff);
    }

    public int getPayloadType() {
        return payloadType;
    }

    public int getSeqNumber() {
        return this.seqNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getVersion() {
        return version;
    }

    public boolean isPadding() {
        return padding;
    }

    public boolean isExtensions() {
        return extensions;
    }

    public int getCc() {
        return cc;
    }

    public long getSsrc() {
        return ssrc;
    }

    public byte[] toByteArray() {
        return bin;
    }

    public boolean getMarker() {
        return this.marker;
    }

    public void setMarker(boolean marker) {
        this.marker = marker;
    }

    protected int append(byte[] data, int offset, int len) {
        int remainder = Math.min(12 - pos, len);
        System.arraycopy(data, offset, bin, pos, remainder);
        pos += remainder;
        if (pos == 12) {
            init(bin);
            filled = true;
        }
        return len - remainder;
    }

    public void init(boolean marker, byte payloadType, int seqNumber, int timestamp, long ssrc) {
        this.payloadType = payloadType;
        this.seqNumber = seqNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.marker = marker;

        bin[0] = (byte) (0x80);

        bin[1] = marker ? (byte) (payloadType | 0x80) : (byte) (payloadType & 0x7f);
        // bin[1] = (payloadType);
        bin[2] = ((byte) ((seqNumber) >> 8));
        bin[3] = ((byte) (seqNumber));

        bin[4] = ((byte) ((timestamp) >> 24));
        bin[5] = ((byte) ((timestamp) >> 16));
        bin[6] = ((byte) ((timestamp) >> 8));
        bin[7] = ((byte) ((timestamp)));

        bin[8] = ((byte) ((ssrc) >> 24));
        bin[9] = ((byte) ((ssrc) >> 16));
        bin[10] = ((byte) ((ssrc) >> 8));
        bin[11] = ((byte) ((ssrc)));
    }

    public void init(byte payloadType, int seqNumber, int timestamp, long ssrc) {
        this.init(false, (byte) (payloadType & 0x7F), seqNumber, timestamp, ssrc);
    }
    
    @Override
    public String toString() {
        return "ssrc=" + this.ssrc + ", timestamp=" + this.timestamp + ", seq=" + this.seqNumber + ", payload=" + this.payloadType;
    }
}
