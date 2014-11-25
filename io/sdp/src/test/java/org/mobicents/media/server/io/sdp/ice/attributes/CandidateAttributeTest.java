package org.mobicents.media.server.io.sdp.ice.attributes;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CandidateAttributeTest {

	@Test
	public void testHostCandidate() {
		// given
		long foundation = 1995739850L;
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
		long foundation = 2162486046L;
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
