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

package org.restcomm.media.core.rtp.netty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.core.rtp.RtpPacket;
import org.restcomm.media.core.rtp.crypto.PacketTransformer;
import org.restcomm.media.core.rtp.netty.SrtpEncoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SrtpEncoderTest {

    @Test
    public void testEncode() {
        // given
        final RtpPacket rtpPacket = mock(RtpPacket.class);
        final PacketTransformer transformer = mock(PacketTransformer.class);
        final SrtpEncoder decoder = new SrtpEncoder(transformer);
        final EmbeddedChannel channel = new EmbeddedChannel(decoder);

        final byte[] encodedPayload = "encoded".getBytes();
        final byte[] decodedPayload = "decoded".getBytes();

        when(rtpPacket.getRawData()).thenReturn(decodedPayload);
        when(transformer.transform(decodedPayload)).thenReturn(encodedPayload);

        // when
        channel.writeOutbound(rtpPacket);
        Object outboundObject = channel.readOutbound();

        // then
        verify(transformer).transform(decodedPayload);
        assertNotNull(outboundObject);
        assertTrue(outboundObject instanceof ByteBuf);
        byte[] data = new byte[encodedPayload.length];
        ((ByteBuf) outboundObject).getBytes(0, data);
        assertEquals(new String(encodedPayload), new String(data));
    }

    @Test
    public void testEncodeFailure() {
        // given
        final RtpPacket rtpPacket = mock(RtpPacket.class);
        final PacketTransformer transformer = mock(PacketTransformer.class);
        final SrtpEncoder decoder = new SrtpEncoder(transformer);
        final EmbeddedChannel channel = new EmbeddedChannel(decoder);

        final byte[] encodedPayload = null;
        final byte[] decodedPayload = "decoded".getBytes();

        when(rtpPacket.getRawData()).thenReturn(decodedPayload);
        when(transformer.transform(decodedPayload)).thenReturn(encodedPayload);

        // when
        channel.writeOutbound(rtpPacket);
        Object outboundObject = channel.readOutbound();

        // then
        verify(transformer).transform(decodedPayload);
        assertNotNull(outboundObject);
        assertTrue(outboundObject instanceof EmptyByteBuf);
    }

}
