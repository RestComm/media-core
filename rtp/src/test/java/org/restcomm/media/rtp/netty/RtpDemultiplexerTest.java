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

package org.restcomm.media.rtp.netty;

import java.io.IOException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;
import org.restcomm.media.pcap.GenericPcapReader;
import org.restcomm.media.pcap.PcapFile;
import org.restcomm.media.rtp.RtpPacket;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.ripe.hadoop.pcap.packet.Packet;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpDemultiplexerTest {

    private static final Logger log = Logger.getLogger(RtpDemultiplexerTest.class);

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
    public void testRtpPacketRecognition() throws Exception {
        // given
        final URL pcapUrl = RtpDemultiplexer.class.getResource("rtp-packet.pcap");
        this.pcapFile = new PcapFile(pcapUrl);
        final RtpDemultiplexer demultiplexer = new RtpDemultiplexer();
        final EmbeddedChannel channel = new EmbeddedChannel(demultiplexer);

        // when
        pcapFile.open();
        final Packet pcapPacket = pcapFile.read();
        byte[] data = (byte[]) pcapPacket.get(GenericPcapReader.PAYLOAD);

        final ByteBuf buffer = Unpooled.wrappedBuffer(data);
        final boolean wrote = channel.writeInbound(buffer);
        final Object packet = channel.readInbound();

        // then
        assertTrue(wrote);
        assertNotNull(packet);
        assertTrue(packet instanceof RtpPacket);

        RtpPacket rtpPacket = (RtpPacket) packet;
        assertEquals(2, rtpPacket.getVersion());
        assertFalse(rtpPacket.hasPadding());
        assertFalse(rtpPacket.hasExtensions());
        assertEquals(0, rtpPacket.getCsrcCount());
        assertEquals(0, rtpPacket.getPayloadType());
        assertFalse(rtpPacket.getMarker());
        assertEquals(1023, rtpPacket.getSeqNumber());
        assertEquals(163680L, rtpPacket.getTimestamp());
        assertEquals((long) 0x3E6E7CB5, rtpPacket.getSyncSource());
    }

}
