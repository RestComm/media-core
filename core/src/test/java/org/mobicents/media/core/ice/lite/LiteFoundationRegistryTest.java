package org.mobicents.media.core.ice.lite;

import org.junit.Test;
import org.mobicents.media.core.ice.HostCandidate;
import org.mobicents.media.core.ice.IceComponent;

import static org.junit.Assert.*;

/**
 * 
 * @author Henrique Rosa
 * 
 */
public class LiteFoundationRegistryTest {

	/**
	 * Tests how the Lite implementation depends solely on the address of a host
	 * candidate to generate a new foundation.
	 */
	@Test
	public void testFoundationRegistry() {
		// given
		LiteFoundationRegistry registry = new LiteFoundationRegistry();
		IceComponent component = new IceComponent(IceComponent.RTP_ID);
		HostCandidate candidate1 = new HostCandidate(component, "192.168.1.65",
				61000);
		HostCandidate candidate2 = new HostCandidate(component, "192.168.1.66",
				61001);
		HostCandidate candidate3 = new HostCandidate(component, "192.168.1.65",
				61002);

		// when
		String foundation1 = registry.assignFoundation(candidate1);
		String foundation2 = registry.assignFoundation(candidate2);
		String foundation3 = registry.assignFoundation(candidate3);

		// then
		assertEquals(String.valueOf(1), foundation1);
		assertEquals(String.valueOf(2), foundation2);
		assertEquals(String.valueOf(1), foundation3);
	}

}
