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
import java.nio.ByteBuffer;

/**
 * 
 * @author Oleg Kulikov
 * @author amit bhayani
 */
public class RtpPacket implements Serializable {

    private int version = 2;
    private boolean padding = false;
    private boolean extensions = false;
    private int cc = 0;
    private volatile boolean marker = false;
    private int payloadType;
    private int seqNumber;
    private long timestamp;
    private long ssrc;
    private byte[] payload;
    private int offset = 0;
    private int length = 0;
    private long time;
    private long duration = -1;
    private boolean isValid = false;
    private byte[] buff;

    public RtpPacket(ByteBuffer readerBuffer) {
        int len = readerBuffer.limit();
        buff = new byte[len];
        readerBuffer.get(buff, 0, len);

        int b = buff[0] & 0xff;

        version = (b & 0xC0) >> 6;
        padding = (b & 0x20) == 0x020;
        extensions = (b & 0x10) == 0x10;
        cc = b & 0x0F;

        b = buff[1] & 0xff;
        marker = (b & 0x80) == 0x80;
        payloadType = b & 0x7F;

        seqNumber = (buff[2] & 0xff) << 8;
        seqNumber = seqNumber | (buff[3] & 0xff);

        timestamp |= buff[4] & 0xFF;
        timestamp <<= 8;
        timestamp |= buff[5] & 0xFF;
        timestamp <<= 8;
        timestamp |= buff[6] & 0xFF;
        timestamp <<= 8;
        timestamp |= buff[7] & 0xFF;

        ssrc = (buff[8] & 0xff);
        ssrc <<= 8;
        ssrc |= (buff[9] & 0xff);
        ssrc <<= 8;
        ssrc |= (buff[10] & 0xff);
        ssrc <<= 8;
        ssrc |= (buff[11] & 0xff);

        payload = new byte[len - 12];
        System.arraycopy(buff, 12, payload, 0, payload.length);
    }

    public RtpPacket(byte payloadType, int seqNumber, int timestamp, long ssrc, byte[] payload) {
        this.payloadType = payloadType;
        this.payload = payload;
        this.seqNumber = seqNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.offset = 0;
        this.length = payload.length;
    }

    public RtpPacket(boolean marker, byte payloadType, int seqNumber, int timestamp, long ssrc, byte[] payload, int offset, int length) {
        this.marker = marker;
        this.payloadType = payloadType;
        this.payload = payload;
        this.seqNumber = seqNumber;
        this.timestamp = timestamp;
        this.ssrc = ssrc;
        this.offset = offset;
        this.length = length;
        this.buff = new byte[payload.length + 12];
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public boolean getMarker() {
        return marker;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public int getSeqNumber() {
        return this.seqNumber;
    }

    public byte[] getPayload() {
        return payload;
    }

    public long getTimestamp() {
        return timestamp;
    }
    //defualt, visible for tests only. and RTP package.
    void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public byte[] toByteArray() {
        buff[0] = (byte) (version << 6);
        if (padding) {
            buff[0] = (byte) (buff[0] | 0x20);
        }

        if (extensions) {
            buff[0] = (byte) (buff[0] | 0x10);
        }

        buff[0] = (byte) (buff[0] | (cc & 0x0f));

        buff[1] = (byte) (payloadType);
        if (marker) {
            System.out.println("Set marker " + marker);
            buff[1] = (byte) (buff[1] | 0x80);
        }

        buff[2] = ((byte) ((seqNumber & 0xFF00) >> 8));
        buff[3] = ((byte) (seqNumber & 0x00FF));

        buff[4] = ((byte) ((timestamp & 0xFF000000) >> 24));
        buff[5] = ((byte) ((timestamp & 0x00FF0000) >> 16));
        buff[6] = ((byte) ((timestamp & 0x0000FF00) >> 8));
        buff[7] = ((byte) ((timestamp & 0x000000FF)));

        buff[8] = ((byte) ((ssrc & 0xFF000000) >> 24));
        buff[9] = ((byte) ((ssrc & 0x00FF0000) >> 16));
        buff[10] = ((byte) ((ssrc & 0x0000FF00) >> 8));
        buff[11] = ((byte) ((ssrc & 0x000000FF)));
        System.arraycopy(payload, offset, buff, 12, length);
        return buff;
    }

    @Override
    public String toString() {
        return "RTP Packet[marker=" + marker + ", seq=" + this.seqNumber + ", timestamp=" + timestamp + ", payload_size=" + payload.length + ", payload=" + payloadType + "]";
    }

    public int getVersion() {
        return version;
    }

    public int getContributingSource() {
        return this.cc;
    }

    public boolean isPadding() {
        return padding;
    }

    public boolean isExtensions() {
        return extensions;
    }

    public long getSyncSource() {
        return this.ssrc;
    }
}
