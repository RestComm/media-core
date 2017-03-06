/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.attributes.AbstractConnectionModeAttribute;
import org.restcomm.media.sdp.attributes.ConnectionModeAttribute;
import org.restcomm.media.sdp.attributes.parser.ConnectionModeAttributeParser;

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
		String sdp1 = "a=sendonly\n\r";
		String sdp2 = "a=recvonly\n\r";
		String sdp3 = "a=sendrecv\n\r";
		String sdp4 = "a=inactive\n\r";
		String sdp5 = "x=inactive\n\r";
		String sdp6 = "a=invalid\n\r";
		String sdp7 = "a=inactive:123\n\r";

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
		String sdp1 = "a=sendonly\n\r";
		
		// when
		ConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.SENDONLY, obj.getKey());
		Assert.assertEquals("sendonly", obj.getKey());
	}
	
	@Test
	public void testParseRecvOnly() throws SdpException {
		// given
		String sdp1 = "a=recvonly\n\r";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.RECVONLY, obj.getKey());
		Assert.assertEquals("recvonly", obj.getKey());
	}
	
	@Test
	public void testParseSendRecv() throws SdpException {
		// given
		String sdp1 = "a=sendrecv\n\r";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.SENDRECV, obj.getKey());
		Assert.assertEquals("sendrecv", obj.getKey());
	}
	
	@Test
	public void testParseInactive() throws SdpException {
		// given
		String sdp1 = "a=inactive\n\r";
		
		// when
		AbstractConnectionModeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.INACTIVE, obj.getKey());
		Assert.assertEquals("inactive", obj.getKey());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=inactive\n\r";
		String sdp2 = "a=recvonly\n\r";
		
		// when
		ConnectionModeAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(ConnectionModeAttribute.RECVONLY, obj.getKey());
		Assert.assertEquals("recvonly", obj.getKey());
	}

}
