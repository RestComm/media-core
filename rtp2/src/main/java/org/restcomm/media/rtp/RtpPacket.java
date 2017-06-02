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

package org.restcomm.media.rtp;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * A data packet consisting of the fixed RTP header, a possibly empty list of contributing sources, and the payload data. Some
 * underlying protocols may require an encapsulation of the RTP packet to be defined. Typically one packet of the underlying
 * protocol contains a single RTP packet, but several RTP packets may be contained if permitted by the encapsulation method
 *
 * @author Oleg Kulikov
 * @author Amit Bhayani
 * @author Ivelin Ivanov (ivelin.ivanov@telestax.com)
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpPacket implements Serializable {

    private static final long serialVersionUID = 967677010497968397L;

    public static final int FIXED_HEADER_SIZE = 12;
    public static final int EXT_HEADER_SIZE = 4;

    /**
     * Current supported RTP version
     */
    public static final int VERSION = 2;

    private final ByteBuffer buffer;

    public RtpPacket(byte[] data) {
        this.buffer = ByteBuffer.wrap(data);
    }

    public RtpPacket(boolean mark, int payloadType, int seqNumber, long timestamp, long ssrc, byte[] data) {
        // Create buffer with necessary length
        this.buffer = ByteBuffer.allocate(FIXED_HEADER_SIZE + data.length);

        // no extensions, paddings and cc
        buffer.put((byte) 0x80);

        byte b = (byte) (payloadType);
        if (mark) {
            b = (byte) (b | 0x80);
        }
        buffer.put(b);

        // sequence number
        buffer.put((byte) ((seqNumber & 0xFF00) >> 8));
        buffer.put((byte) (seqNumber & 0x00FF));

        // timestamp
        buffer.put((byte) ((timestamp & 0xFF000000) >> 24));
        buffer.put((byte) ((timestamp & 0x00FF0000) >> 16));
        buffer.put((byte) ((timestamp & 0x0000FF00) >> 8));
        buffer.put((byte) ((timestamp & 0x000000FF)));

        // ssrc
        buffer.put((byte) ((ssrc & 0xFF000000) >> 24));
        buffer.put((byte) ((ssrc & 0x00FF0000) >> 16));
        buffer.put((byte) ((ssrc & 0x0000FF00) >> 8));
        buffer.put((byte) ((ssrc & 0x000000FF)));

        buffer.put(data);
        buffer.flip();
    }

    /**
     * Gets the full length of the packet.
     * 
     * @return The lenght of the packet.
     */
    public int getLength() {
        return buffer.limit();
    }

    /**
     * Gets the RTP header length.
     *
     * @return The RTP header length.
     */
    public int getHeaderLength() {
        if (hasExtensions()) {
            return FIXED_HEADER_SIZE + 4 * getContributingSource() + EXT_HEADER_SIZE + getExtensionLength();
        }
        return FIXED_HEADER_SIZE + 4 * getContributingSource();
    }

    /**
     * Gets the version field.
     *
     * <p>
     * This field identifies the version of RTP. The version defined by this specification is two (2).<br>
     * (The value 1 is used by the first draft version of RTP and the value 0 is used by the protocol initially implemented in
     * the "vat" audio tool.)
     * </p>
     * 
     * @return the version value.
     */
    public int getVersion() {
        return (buffer.get(0) & 0xC0) >> 6;
    }

    /**
     * Gets the Padding bit.
     *
     * <p>
     * If the padding bit is set, the packet contains one or more additional padding octets at the end which are not part of the
     * payload.<br>
     * The last octet of the padding contains a count of how many padding octets should be ignored.
     * </p>
     * <p>
     * Padding may be needed by some encryption algorithms with fixed block sizes or for carrying several RTP packets in a
     * lower-layer protocol data unit.
     * </p>
     * 
     * @return true if padding bit set.
     */
    public boolean hasPadding() {
        return (buffer.get(0) & 0x20) == 0x020;
    }

    /**
     * Gets the RTP padding size.
     *
     * @return The padding size.
     */
    public int getPaddingSize() {
        if ((buffer.get(0) & 0x4) == 0) {
            return 0;
        }
        return buffer.get(buffer.limit() - 1);
    }

    /**
     * Gets the Extension bit.
     *
     * <p>
     * If the extension bit is set, the fixed header is followed by exactly one header extension.
     * </p>
     * 
     * @return true if extension bit set.
     */
    public boolean hasExtensions() {
        return (buffer.get(0) & 0x10) == 0x010;
    }

    /**
     * Returns the length of the extensions currently added to this packet.
     *
     * @return the length of the extensions currently added to this packet.
     */
    public int getExtensionLength() {
        if (!hasExtensions()) {
            return 0;
        }

        // the extension length comes after the RTP header, the CSRC list, and
        // after two bytes in the extension header called "defined by profile"
        int extLenIndex = FIXED_HEADER_SIZE + getContributingSource() * 4 + 2;
        return ((buffer.get(extLenIndex) << 8) | buffer.get(extLenIndex + 1) * 4);
    }

    /**
     * Gets the Contributing source field.
     *
     * <p>
     * The CSRC list identifies the contributing sources for the payload contained in this packet.
     * </p>
     * <p>
     * The number of identifiers is given by the CC field. If there are more than 15 contributing sources, only 15 may be
     * identified. CSRC identifiers are inserted by mixers, using the SSRC identifiers of contributing sources.
     * </p>
     * <p>
     * For example, for audio packets the SSRC identifiers of all sources that were mixed together to create a packet are
     * listed, allowing correct talker indication at the receiver.
     * </p>
     * 
     * @return synchronization source.
     */
    public int getContributingSource() {
        return buffer.get(0) & 0x0F;
    }

    /**
     * Gets the Marker bit.
     *
     * <p>
     * The interpretation of the marker is defined by a profile. It is intended to allow significant events such as frame
     * boundaries to be marked in the packet stream.
     * </p>
     * <p>
     * A profile may define additional marker bits or specify that there is no marker bit by changing the number of bits in the
     * payload type field
     * </p>
     * 
     * @return true if marker set.
     */
    public boolean getMarker() {
        return (buffer.get(1) & 0xff & 0x80) == 0x80;
    }

    /**
     * Gets the Payload Type.
     *
     * <p>
     * This field identifies the format of the RTP payload and determines its interpretation by the application.
     * </p>
     * <p>
     * A profile specifies a default static mapping of payload type codes to payload formats. Additional payload type codes may
     * be defined dynamically through non-RTP means.
     * </p>
     *
     * @return integer value of payload type.
     */
    public int getPayloadType() {
        return (buffer.get(1) & 0xff & 0x7f);
    }

    /**
     * Gets the Sequence Number.
     *
     * <p>
     * The sequence number increments by one for each RTP data packet sent, and may be used by the receiver to detect packet
     * loss and to restore packet sequence.
     * </p>
     * <p>
     * The initial value of the sequence number is random (unpredictable) to make known-plaintext attacks on encryption more
     * difficult, even if the source itself does not encrypt, because the packets may flow through a translator that does.
     * </p>
     *
     * @return the sequence number value.
     */
    public int getSequenceNumber() {
        return buffer.getShort(2) & 0xFFFF;
    }

    /**
     * Get the Timestamp.
     * <p>
     * The timestamp reflects the sampling instant of the first octet in the RTP data packet.
     * </p>
     * <p>
     * The sampling instant must be derived from a clock that increments monotonically and linearly in time to allow
     * synchronization and jitter calculations.
     * </p>
     * <p>
     * The resolution of the clock must be sufficient for the desired synchronization accuracy and for measuring packet arrival
     * jitter (one tick per video frame is typically not sufficient).
     * </p>
     * <p>
     * The clock frequency is dependent on the format of data carried as payload and is specified statically in the profile or
     * payload format specification that defines the format, or may be specified dynamically for payload formats defined through
     * non-RTP means.
     * </p>
     * <p>
     * If RTP packets are generated periodically, the nominal sampling instant as determined from the sampling clock is to be
     * used, not a reading of the system clock.
     * </p>
     * <p>
     * As an example, for fixed-rate audio the timestamp clock would likely increment by one for each sampling period. If an
     * audio application reads blocks covering 160 sampling periods from the input device, the timestamp would be increased by
     * 160 for each such block, regardless of whether the block is transmitted in a packet or dropped as silent.
     * </p>
     * <p>
     * The initial value of the timestamp is random, as for the sequence number. Several consecutive RTP packets may have equal
     * timestamps if they are (logically) generated at once, e.g., belong to the same video frame. Consecutive RTP packets may
     * contain timestamps that are not monotonic if the data is not transmitted in the order it was sampled, as in the case of
     * MPEG interpolated video frames. (The sequence numbers of the packets as transmitted will still be monotonic.)
     * </p>
     * 
     * @return timestamp value
     */
    public long getTimestamp() {
        return ((long) (buffer.get(4) & 0xff) << 24) | ((long) (buffer.get(5) & 0xff) << 16)
                | ((long) (buffer.get(6) & 0xff) << 8) | ((long) (buffer.get(7) & 0xff));
    }

    /**
     * Gets the Synchronization Source.
     *
     * <p>
     * The SSRC field identifies the synchronization source. This identifier is chosen randomly, with the intent that no two
     * synchronization sources within the same RTP session will have the same SSRC identifier.
     * <p>
     * Although the probability of multiple sources choosing the same identifier is low, all RTP implementations must be
     * prepared to detect and resolve collisions.
     * </p>
     * <p>
     * Section 8 describes the probability of collision along with a mechanism for resolving collisions and detecting RTP-level
     * forwarding loops based on the uniqueness of the SSRC identifier. If a source changes its source transport address, it
     * must also choose a new SSRC identifier to avoid being interpreted as a looped source.
     * </p>
     * 
     * @return the sysncronization source
     */
    public long getSynchronizationSource() {
        return readUnsignedIntAsLong(8);
    }

    /**
     * Reads the data transported by RTP in a packet, such as audio samples or compressed video data.
     *
     * @param buff the buffer used for reading
     * @param offset the initial offset inside buffer.
     */
    public void getPayload(byte[] buff, int offset) {
        buffer.position(FIXED_HEADER_SIZE);
        buffer.get(buff, offset, buffer.limit() - FIXED_HEADER_SIZE);
        buffer.rewind();
    }

    /**
     * Reads the data transported by RTP in a packet, such as audio samples or compressed video data.
     *
     * @param buff the buffer used for reading
     */
    public void getPayload(byte[] buff) {
        getPayload(buff, 0);
    }

    /**
     * Reads the data transported by RTP in a packet, such as audio samples or compressed video data.
     *
     * @return buff the buffer used for reading
     */
    public byte[] getPayload() {
        byte[] buff = new byte[getPayloadLength()];
        getPayload(buff);
        return buff;
    }

    /**
     * Gets the RTP payload length.
     *
     * @return The length of the payload.
     */
    public int getPayloadLength() {
        return buffer.limit() - getHeaderLength();
    }

    /**
     * Reads an unsigned integer as long at specified offset.
     *
     * @param off start offset of this unsigned integer
     * @return unsigned integer as long at offset
     */
    private long readUnsignedIntAsLong(int off) {
        buffer.position(off);
        long value = (((long) (buffer.get() & 0xff) << 24) | ((long) (buffer.get() & 0xff) << 16)
                | ((long) (buffer.get() & 0xff) << 8) | ((long) (buffer.get() & 0xff))) & 0xFFFFFFFFL;
        buffer.rewind();
        return value;
    }
}
