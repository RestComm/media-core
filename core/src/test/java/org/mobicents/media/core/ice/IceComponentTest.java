package org.mobicents.media.core.ice;

public class IceComponentTest {

	private long calculatePriority(int preference, int precedence,
			int componentId) {
		return (long) (preference << 24) + (long) (precedence << 8)
				+ (long) (256 - componentId);
	}

}
