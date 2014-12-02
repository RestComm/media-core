package org.mobicents.media.server.io.sdp.rtcp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtcpAttributeParserTest {
	
	private static final RtcpAttributeParser parser = new RtcpAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtcp:65000";
		String sdp2 = "x=rtcp:65000";
		String sdp3 = "a=rtcp:xyz";
		String sdp4 = "a=rtcp:65000 xyz";
		
		String sdp5 = "a=rtcp:65000 IN IP4 127.0.0.1";
		String sdp6 = "a=rtcp:65000 IP4 127.0.0.1";
		String sdp7 = "a=rtcp:65000 IN 127.0.0.1";
		String sdp8 = "a=rtcp:65000 IN IP4 xyz";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		boolean canParseSdp7 = parser.canParse(sdp7);
		boolean canParseSdp8 = parser.canParse(sdp8);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertTrue(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
		Assert.assertFalse(canParseSdp8);
	}

	@Test
	public void testParseSimpleAttribute() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertNull(obj.getNetworkType());
		Assert.assertNull(obj.getAddressType());
		Assert.assertNull(obj.getAddress());
	}

	@Test
	public void testParseComplexAttribute() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000 IN IP4 127.0.0.1";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertEquals("IN", obj.getNetworkType());
		Assert.assertEquals("IP4", obj.getAddressType());
		Assert.assertEquals("127.0.0.1", obj.getAddress());
	}

	@Test
	public void testParseSimpleToComplexOverwrite() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000";
		String sdp2 = "a=rtcp:64000 IN IP4 127.0.0.1";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(64000, obj.getPort());
		Assert.assertEquals("IN", obj.getNetworkType());
		Assert.assertEquals("IP4", obj.getAddressType());
		Assert.assertEquals("127.0.0.1", obj.getAddress());
	}

	@Test
	public void testParseComplexToSimpleOverwrite() throws SdpException {
		// given
		String sdp1 = "a=rtcp:64000 IN IP4 127.0.0.1";
		String sdp2 = "a=rtcp:65000";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertNull(obj.getNetworkType());
		Assert.assertNull(obj.getAddressType());
		Assert.assertNull(obj.getAddress());
	}

}
