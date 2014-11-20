package org.mobicents.media.server.io.sdp.fields.attributes.ice;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CandidateAttributeTest {
	/*
	 * a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0<br>
	 * a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0<br>
	 * a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr 85.241.121.60 rport 55027 generation 0
	 */
	
	public void testCanParse() {
		// given
		String validHost = "a=candidate:1995739850 1 udp 2113937151 192.168.1.65 54550 typ host generation 0";
		String validSrflx = "a=candidate:2162486046 1 udp 1845501695 85.241.121.60 60495 typ srflx raddr 192.168.1.65 rport 54550 generation 0";
		String validRelay = "a=candidate:2564697628 1 udp 33562367 75.126.93.124 53056 typ relay raddr 85.241.121.60 rport 55027 generation 0";
		
		// when
		
		// then
	}
}
