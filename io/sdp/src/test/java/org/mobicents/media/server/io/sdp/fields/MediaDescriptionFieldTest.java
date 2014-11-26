package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.MediaProfile;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaDescriptionFieldTest {
	
	@Test
	public void testCustomMediaDescription() {
		// given
		String media = "audio";
		int port = 65535;
		String protocol = "RTP/AVP";
		int[] formats = new int[] { 0, 101 };
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.setMedia(media);
		md.setPort(port);
		md.setProtocol(MediaProfile.RTP_AVP);
		md.addFormats(formats);
		
		// then
		Assert.assertEquals(media, md.getMedia());
		Assert.assertEquals(port, md.getPort());
		Assert.assertEquals(protocol, md.getProtocol());
		Assert.assertTrue(md.containsFormat(0));
		Assert.assertTrue(md.containsFormat(101));
		Assert.assertFalse(md.containsFormat(126));
	}
	
}