package org.mobicents.media.server.io.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.AbstractConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionModeAttributeParserTest {

	private final ConnectionModeAttributeParser parser = new ConnectionModeAttributeParser();

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=sendonly";
		String sdp2 = "a=recvonly";
		String sdp3 = "a=sendrecv";
		String sdp4 = "a=inactive";
		String sdp5 = "x=inactive";
		String sdp6 = "a=invalid";
		String sdp7 = "a=inactive:123";

		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		boolean canParseSdp7 = parser.canParse(sdp7);

		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertTrue(canParseSdp3);
		Assert.assertTrue(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}

	@Test
	public void testParseSendOnly() throws SdpException {
		// given
		String sdp1 = "a=sendonly";
		
		// when
		ConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.SENDONLY, obj.getKey());
		Assert.assertEquals("sendonly", obj.getKey());
	}
	
	@Test
	public void testParseRecvOnly() throws SdpException {
		// given
		String sdp1 = "a=recvonly";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.RECVONLY, obj.getKey());
		Assert.assertEquals("recvonly", obj.getKey());
	}
	
	@Test
	public void testParseSendRecv() throws SdpException {
		// given
		String sdp1 = "a=sendrecv";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.SENDRECV, obj.getKey());
		Assert.assertEquals("sendrecv", obj.getKey());
	}
	
	@Test
	public void testParseInactive() throws SdpException {
		// given
		String sdp1 = "a=inactive";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.INACTIVE, obj.getKey());
		Assert.assertEquals("inactive", obj.getKey());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=inactive";
		String sdp2 = "a=recvonly";
		
		// when
		ConnectionModeAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.RECVONLY, obj.getKey());
		Assert.assertEquals("recvonly", obj.getKey());
	}

}
