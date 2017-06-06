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

package org.restcomm.media.rtp.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.RtpStatistics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpEncoderTest {

    @Test
    public void testEncode() {
        // given
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final RtpPacketEncoder encoder = new RtpPacketEncoder(statistics);
        final EmbeddedChannel channel = new EmbeddedChannel(encoder);

        // when
        final RtpPacket packet = new RtpPacket(true, 8, 100, 123456789, 160 * 5, new byte[160]);
        channel.writeOutbound(packet);
        Object outbound = channel.readOutbound();

        // then
        assertNotNull(outbound);
        assertTrue(outbound instanceof ByteBuf);
        ByteBuf buf = (ByteBuf) outbound;
        assertEquals(packet.getLength(), buf.readableBytes());
        verify(statistics).outgoingRtp(packet);
    }

}
