/**
 * 
 */
package org.mobicents.media.server.impl.rtp.sdp;

import org.junit.Assert;
import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.parser.MediaDescriptionFieldParser;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpFactoryTest {
	
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

}
