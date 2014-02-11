package org.mobicents.media.core.ice.network.stun;

import java.nio.channels.SelectionKey;

public interface StunListener {

	void onSuccessfulResponse(SelectionKey key);
	
}
