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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.restcomm.media.pcap.GenericPcapReader;
import org.restcomm.media.pcap.PcapFile;

import net.ripe.hadoop.pcap.packet.Packet;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpPacketTest {

    private static final Logger log = Logger.getLogger(RtpPacketTest.class);

    private PcapFile pcapFile;

    @After
    public void after() {
        if (pcapFile != null) {
            if (!pcapFile.isComplete()) {
                try {
                    pcapFile.close();
                } catch (IOException e) {
                    log.warn("Could not close PCAP file " + pcapFile.getPath().toString(), e);
                }
            }
            pcapFile = null;
        }
    }

    @Test
    public void testWrapRawData() throws Exception {
        // given
        final File file = new File("src/test/resources/pcap/rtp-packet.pcap");
        final URL pcapUrl = file.toURI().toURL();
        this.pcapFile = new PcapFile(pcapUrl);

        // when
        pcapFile.open();
        final Packet pcapPacket = pcapFile.read();
        byte[] data = (byte[]) pcapPacket.get(GenericPcapReader.PAYLOAD);

        RtpPacket rtpPacket = new RtpPacket(data);

        // then
        assertEquals(2, rtpPacket.getVersion());
        assertFalse(rtpPacket.hasPadding());
        assertFalse(rtpPacket.hasExtensions());
        assertEquals(0, rtpPacket.getContributingSource());
        assertEquals(0, rtpPacket.getPayloadType());
        assertFalse(rtpPacket.getMarker());
        assertEquals(1023, rtpPacket.getSequenceNumber());
        assertEquals(163680L, rtpPacket.getTimestamp());
        assertEquals((long) 0x3E6E7CB5, rtpPacket.getSynchronizationSource());
    }

    @Test
    public void testWrapConcreteData() throws Exception {
        // given
        byte[] data = "data".getBytes();
        long ssrc = 123456789L;
        long timestamp = 987654321L;
        int seqNumber = 1111;
        int payloadType = 8;
        boolean mark = false;

        // when
        RtpPacket rtpPacket = new RtpPacket(mark, payloadType, seqNumber, timestamp, ssrc, data);

        // then
        assertEquals(2, rtpPacket.getVersion());
        assertFalse(rtpPacket.hasPadding());
        assertFalse(rtpPacket.hasExtensions());
        assertEquals(0, rtpPacket.getContributingSource());
        assertEquals(payloadType, rtpPacket.getPayloadType());
        assertEquals(mark, rtpPacket.getMarker());
        assertEquals(seqNumber, rtpPacket.getSequenceNumber());
        assertEquals(timestamp, rtpPacket.getTimestamp());
        assertEquals(ssrc, rtpPacket.getSynchronizationSource());
        assertEquals(data.length, rtpPacket.getPayloadLength());
        assertEquals(new String(data), new String(rtpPacket.getPayload()));
    }

}
