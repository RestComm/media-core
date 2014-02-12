package org.mobicents.media.core.ice.network.stun.message.attributes.address;

import org.mobicents.media.core.ice.network.stun.message.attributes.StunAttribute;

/**
 * The XOR-PEER-ADDRESS attribute is given by a TURN client to indicates the
 * peer destination address of its relayed packet.
 * 
 * It has the same format as XOR-MAPPED-ADDRESS.
 */
public class XorPeerAddressAttribute extends XorMappedAddressAttribute {

	public static final String NAME = "XOR-PEER-ADDRESS";

	XorPeerAddressAttribute() {
		super(StunAttribute.XOR_PEER_ADDRESS);
	}

	@Override
	public String getName() {
		return NAME;
	}
}
