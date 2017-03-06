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

package org.restcomm.media.sdp.ice.attributes;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.ice.attributes.CandidateAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CandidateAttributeTest {

	@Test
	public void testHostCandidate() {
		// given
		String foundation = "1995739850";
		short componentId = 1;
		String protocol = "udp";
		int priority = 2113937151;
		String address = "192.168.1.65";
		int port = 54550;
		String type = "host";
		int generation = 0;
		String sdp = "a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0";
		
		// when
		CandidateAttribute obj = new CandidateAttribute();
		obj.setFoundation(foundation);
		obj.setComponentId(componentId);
		obj.setProtocol(protocol);
		obj.setPriority(priority);
		obj.setAddress(address);
		obj.setPort(port);
		obj.setCandidateType(type);
		obj.setGeneration(generation);
		
		// then
		Assert.assertEquals(sdp, obj.toString());
	}

	@Test
	public void testSrflxCandidate() {
		// given
		String foundation = "2162486046";
		short componentId = 1;
		String protocol = "udp";
		int priority = 1845501695;
		String address = "85.241.121.60";
		int port = 60495;
		String type = "srflx";
		String raddr = "192.168.1.65";
		int rport = 54550;
		int generation = 0;
		String sdp = "a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0";
		
		// when
		CandidateAttribute obj = new CandidateAttribute();
		obj.setFoundation(foundation);
		obj.setComponentId(componentId);
		obj.setProtocol(protocol);
		obj.setPriority(priority);
		obj.setAddress(address);
		obj.setPort(port);
		obj.setCandidateType(type);
		obj.setRelatedAddress(raddr);
		obj.setRelatedPort(rport);
		obj.setGeneration(generation);
		
		// then
		Assert.assertEquals(sdp, obj.toString());
	}

}
