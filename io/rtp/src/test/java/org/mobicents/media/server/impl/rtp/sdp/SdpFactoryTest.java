/**
 * 
 */
package org.mobicents.media.server.impl.rtp.sdp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.parser.MediaDescriptionFieldParser;
import org.mobicents.media.server.io.sdp.format.RTPFormat;
import org.mobicents.media.server.io.sdp.format.RTPFormats;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.Format;
import org.mobicents.media.server.spi.format.FormatFactory;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpFactoryTest {

    private final String localAddress = "127.0.0.1";
    private final String externalAddress = "external.address.com";
    private final String rtpAddress = "rtp.address.com";
    private final String rtcpAddress = "rtcp.address.com";
	
	@Test
	public void testRejectApplication() throws SdpException {
		// given
		String applicationSdp = "m=application 9 DTLS/SCTP 5000";
		MediaDescriptionFieldParser mediaFieldParser = new MediaDescriptionFieldParser();
		MediaDescriptionField applicationField = mediaFieldParser.parse(applicationSdp);
		
		SessionDescription answer = new SessionDescription();
		
		// when
		SdpFactory.rejectMediaField(answer, applicationField);
		
		// then
		MediaDescriptionField mediaDescription = answer.getMediaDescription("application");
		Assert.assertNotNull(mediaDescription);
		Assert.assertEquals(0, mediaDescription.getPort());
		Assert.assertEquals("DTLS/SCTP", mediaDescription.getProtocol());
		int[] payloadTypes = mediaDescription.getPayloadTypes();
		Assert.assertEquals(1, payloadTypes.length);
		Assert.assertEquals(5000, payloadTypes[0]);
	}

    @Test
    public void testExternalAddress() {
        // given
        AudioChannel audioChannel = mock(AudioChannel.class);

        // when
        Format audioFormat = FormatFactory.createAudioFormat(new EncodingName("audio"));
        RTPFormat rtpFormat = new RTPFormat(0, audioFormat);
        RTPFormats rtpFormats = new RTPFormats();
        rtpFormats.add(rtpFormat);
        when(audioChannel.getExternalAddress()).thenReturn(this.externalAddress);
        when(audioChannel.getRtpAddress()).thenReturn(this.rtpAddress);
        when(audioChannel.getRtcpAddress()).thenReturn(this.rtcpAddress);
        when(audioChannel.isIceEnabled()).thenReturn(false);
        when(audioChannel.isRtcpMux()).thenReturn(false);
        when(audioChannel.getFormats()).thenReturn(rtpFormats);
        when(audioChannel.getMediaType()).thenReturn(AudioChannel.MEDIA_TYPE);

        // then test build SDP with valid externalAddress
        SessionDescription sd = SdpFactory.buildSdp(true, this.localAddress, this.externalAddress, audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.externalAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.externalAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.externalAddress));

        // when
        when(audioChannel.isIceEnabled()).thenReturn(true);
        when(audioChannel.isRtcpMux()).thenReturn(true);

        // then test build SDP with valid externalAddress, ICE and RTCP-MUX
        sd = SdpFactory.buildSdp(true, this.localAddress, this.externalAddress, audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.externalAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.externalAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.externalAddress));

        // when
        when(audioChannel.getExternalAddress()).thenReturn("");
        when(audioChannel.isIceEnabled()).thenReturn(false);
        when(audioChannel.isRtcpMux()).thenReturn(false);

        // then test build SDP with empty externalAddress
        sd = SdpFactory.buildSdp(true, this.localAddress, "", audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.localAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtcpAddress));

        // when
        when(audioChannel.isIceEnabled()).thenReturn(true);
        when(audioChannel.isRtcpMux()).thenReturn(true);

        // then test build SDP with empty externalAddress, ICE and RTCP-MUX
        sd = SdpFactory.buildSdp(true, this.localAddress, "", audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtpAddress));

        // when
        when(audioChannel.getExternalAddress()).thenReturn(null);
        when(audioChannel.isIceEnabled()).thenReturn(false);
        when(audioChannel.isRtcpMux()).thenReturn(false);

        // then test build SDP with null externalAddress
        sd = SdpFactory.buildSdp(true, this.localAddress, null, audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.localAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtcpAddress));

        // when
        when(audioChannel.isIceEnabled()).thenReturn(true);
        when(audioChannel.isRtcpMux()).thenReturn(true);

        // then test build SDP with null externalAddress, ICE and RTCP-MUX
        sd = SdpFactory.buildSdp(true, this.localAddress, null, audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(this.rtpAddress));
        Assert.assertTrue(sd.getMediaDescription("audio").getRtcp().getAddress().equals(this.rtpAddress));
    }

}
