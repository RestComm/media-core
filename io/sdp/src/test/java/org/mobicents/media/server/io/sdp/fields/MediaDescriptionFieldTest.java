package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.MediaProfile;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;

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
		short[] formats = new short[] { 0, 101 };
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.setMedia(media);
		md.setPort(port);
		md.setProtocol(MediaProfile.RTP_AVP);
		md.setPayloadTypes(formats);
		
		// then
		Assert.assertEquals(media, md.getMedia());
		Assert.assertEquals(port, md.getPort());
		Assert.assertEquals(protocol, md.getProtocol());
		Assert.assertTrue(md.containsPayloadType((short) 0));
		Assert.assertTrue(md.containsPayloadType((short) 101));
		Assert.assertFalse(md.containsPayloadType((short) 126));
	}

	@Test
	public void testSessionLevelAccessor() {
		// given
		SessionDescription session = new SessionDescription();
		IcePwdAttribute sessionIcePwd = new IcePwdAttribute("password");
		session.setIcePwd(sessionIcePwd);
		IceUfragAttribute sessionIceUfrag = new IceUfragAttribute("ufrag");
		session.setIceUfrag(sessionIceUfrag);
		FingerprintAttribute sessionFingerprint = new FingerprintAttribute("sha-256", "35:A6:57");
		session.setFingerprint(sessionFingerprint);
		
		// when
		MediaDescriptionField media = new MediaDescriptionField(session);
		IcePwdAttribute mediaIcePwd = new IcePwdAttribute("password2");
		media.setIcePwd(mediaIcePwd);
		IceUfragAttribute mediaIceUfrag = new IceUfragAttribute("ufrag2");
		media.setIceUfrag(mediaIceUfrag);
		
		// then
		Assert.assertEquals(mediaIcePwd, media.getIcePwd());
		Assert.assertEquals(mediaIceUfrag, media.getIceUfrag());
		Assert.assertEquals(sessionFingerprint, media.getFingerprint());
	}
}