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

package org.restcomm.media.rtp.sdp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.fields.parser.MediaDescriptionFieldParser;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpBuilderTest {

    @Test
    public void testRejectApplication() throws SdpException {
        // given
        final String applicationSdp = "m=application 9 DTLS/SCTP 5000";
        final MediaDescriptionFieldParser mediaFieldParser = new MediaDescriptionFieldParser();
        final MediaDescriptionField applicationField = mediaFieldParser.parse(applicationSdp);
        final SessionDescription answer = new SessionDescription();
        final SdpBuilder sdpBuilder = new SdpBuilder();

        // when
        sdpBuilder.rejectMediaField(answer, applicationField);

        // then
        MediaDescriptionField mediaDescription = answer.getMediaDescription("application");
        assertNotNull(mediaDescription);
        assertEquals(0, mediaDescription.getPort());
        assertEquals("DTLS/SCTP", mediaDescription.getProtocol());
        String[] payloadTypes = mediaDescription.getPayloadTypes();
        assertEquals(1, payloadTypes.length);
        assertEquals("5000", payloadTypes[0]);
    }

    // TODO Test supported formats

    @Test
    public void testExternalAddress() {
        // given
        final String cname = "cname";
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final ConnectionMode mode = ConnectionMode.RECV_ONLY;
        final RTPFormats supportedFormats = AVProfile.audio;
        final String localAddress = "192.168.1.64";
        final String externalAddress = "123.123.123.123";
        final SocketAddress rtpAddress = new InetSocketAddress(localAddress, 6000);
        final RtpSession audioSession = mock(RtpSession.class);

        when(audioSession.getMediaType()).thenReturn(mediaType);
        when(audioSession.getMode()).thenReturn(mode);
        when(audioSession.getRtpAddress()).thenReturn(rtpAddress);
        when(audioSession.getSsrc()).thenReturn(ssrc);
        when(audioSession.getFormats()).thenReturn(supportedFormats);

        final SdpBuilder sdpBuilder = new SdpBuilder();

        // when
        SessionDescription sdp = sdpBuilder.buildSessionDescription(true, cname, localAddress, externalAddress, audioSession);

        // then
        assertEquals(externalAddress, sdp.getConnection().getAddress());
        assertEquals(externalAddress, sdp.getMediaDescription("audio").getConnection().getAddress());
        // TODO assertEquals(externalAddress, sdp.getMediaDescription("audio").getRtcp().getAddress());
    }

    @Test
    public void testEmptyExternalAddress() {
        // given
        final String cname = "cname";
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final ConnectionMode mode = ConnectionMode.RECV_ONLY;
        final RTPFormats supportedFormats = AVProfile.audio;
        final String localAddress = "192.168.1.64";
        final String externalAddress = "";
        final SocketAddress rtpAddress = new InetSocketAddress(localAddress, 6000);
        final RtpSession audioSession = mock(RtpSession.class);

        when(audioSession.getMediaType()).thenReturn(mediaType);
        when(audioSession.getMode()).thenReturn(mode);
        when(audioSession.getRtpAddress()).thenReturn(rtpAddress);
        when(audioSession.getSsrc()).thenReturn(ssrc);
        when(audioSession.getFormats()).thenReturn(supportedFormats);

        final SdpBuilder sdpBuilder = new SdpBuilder();

        // when
        SessionDescription sdp = sdpBuilder.buildSessionDescription(true, cname, localAddress, externalAddress, audioSession);

        // then
        assertEquals(localAddress, sdp.getConnection().getAddress());
        assertEquals(localAddress, sdp.getMediaDescription("audio").getConnection().getAddress());
        // TODO Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtcpAddress));
    }

    @Test
    public void testNullExternalAddress() {
        // given
        final String cname = "cname";
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final ConnectionMode mode = ConnectionMode.RECV_ONLY;
        final RTPFormats supportedFormats = AVProfile.audio;
        final String localAddress = "192.168.1.64";
        final String externalAddress = null;
        final SocketAddress rtpAddress = new InetSocketAddress(localAddress, 6000);
        final RtpSession audioSession = mock(RtpSession.class);

        when(audioSession.getMediaType()).thenReturn(mediaType);
        when(audioSession.getMode()).thenReturn(mode);
        when(audioSession.getRtpAddress()).thenReturn(rtpAddress);
        when(audioSession.getSsrc()).thenReturn(ssrc);
        when(audioSession.getFormats()).thenReturn(supportedFormats);

        final SdpBuilder sdpBuilder = new SdpBuilder();

        // when
        SessionDescription sdp = sdpBuilder.buildSessionDescription(true, cname, localAddress, externalAddress, audioSession);

        // then
        assertEquals(localAddress, sdp.getConnection().getAddress());
        assertEquals(localAddress, sdp.getMediaDescription("audio").getConnection().getAddress());
        // TODO Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtcpAddress));
    }

}
