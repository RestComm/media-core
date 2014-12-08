package org.mobicents.media.server.io.sdp.attributes.parser;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SsrcAttributeParserTest {
	
	private final SsrcAttributeParser parser = new SsrcAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1993357196 xyz\n\r";
		String sdp3 = "x=ssrc:1993357196 xyz\n\r";
		String sdp4 = "a=ssrc:1993357196\n\r";
		String sdp5 = "a=ssrc:1993357196 cname:\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
	}

	@Test
	public void testParseSimpleAttribute() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 xyz\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertNull(ssrc.getAttributeValue("xyz"));
	}

	@Test
	public void testParseComplexAttribute() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);

		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", ssrc.getAttributeValue("cname"));
	}
	
	@Test
	public void testParseOverwriteSameSsrc() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1993357196 label:753ad6b1-31a6-4fea-8070-269c6df6ff0e\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		parser.parse(ssrc, sdp2);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", ssrc.getAttributeValue("cname"));
		Assert.assertEquals("753ad6b1-31a6-4fea-8070-269c6df6ff0e", ssrc.getAttributeValue("label"));
	}

	@Test
	public void testParseOverwriteNewSsrc() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1111111111 label:753ad6b1-31a6-4fea-8070-269c6df6ff0e\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		parser.parse(ssrc, sdp2);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1111111111", ssrc.getSsrcId());
		Assert.assertNull(ssrc.getAttributeValue("cname"));
		Assert.assertEquals("753ad6b1-31a6-4fea-8070-269c6df6ff0e", ssrc.getAttributeValue("label"));
	}

}
